package com.yin.cita.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yin.cita.model.DocumentElement;
import com.yin.cita.model.ChunkMetadata;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

@Service
public class ChunkingService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_CHUNK_SIZE = 2000;
    private static final int OVERLAP = 200;

    @Autowired
    private MetadataEnrichmentService enrichmentService;

    // Helper class to track headers
    private static class HeaderNode {
        int level;
        String text;

        HeaderNode(int level, String text) {
            this.level = level;
            this.text = text;
        }
    }

    public List<Map<String, Object>> chunkElements(List<DocumentElement> elements) {
        List<Map<String, Object>> chunks = new ArrayList<>();

        StringBuilder currentText = new StringBuilder();
        Map<String, Object> currentMetadata = new HashMap<>();
        Set<String> currentPageNumbers = new HashSet<>();

        // Stack to track header hierarchy (H1 -> H2 -> H3)
        Stack<HeaderNode> headerStack = new Stack<>();

        for (DocumentElement element : elements) {
            String text = element.getText();
            DocumentElement.Type type = element.getType();

            // Collect page number
            String pageNum = (String) element.getMetadata().get("page_number");
            if (pageNum != null) {
                currentPageNumbers.add(pageNum);
            }

            if (type == DocumentElement.Type.TITLE) {
                // A new title means the previous section is done.
                if (currentText.length() > 0) {
                    finalizeChunk(chunks, currentText, currentMetadata, buildContextString(headerStack),
                            currentPageNumbers);
                }

                // Update Hierarchy
                String tag = (String) element.getMetadata().getOrDefault("tag", "h1");
                int level = Integer.parseInt(tag.substring(1));

                while (!headerStack.isEmpty() && headerStack.peek().level >= level) {
                    headerStack.pop();
                }
                headerStack.push(new HeaderNode(level, text));

                // Start new chunk logic
                currentText.append(text).append("\n");
                currentMetadata.putAll(element.getMetadata());

            } else if (type == DocumentElement.Type.TABLE) {
                // Finalize text accumulated so far
                if (currentText.length() > 0) {
                    finalizeChunk(chunks, currentText, currentMetadata, buildContextString(headerStack),
                            currentPageNumbers);
                }

                // Create atomic table chunk WITH context
                Map<String, Object> tableChunk = new HashMap<>();
                String context = buildContextString(headerStack);
                String sectionHeader = context.replace("[Context: ", "").replace("]", "");

                // Inject context into text for searchability
                String chunkContent = context + "\n" + text;
                tableChunk.put("text", chunkContent);
                tableChunk.put("metadata", new HashMap<>(element.getMetadata())); // Copy metadata
                tableChunk.put("type", "TABLE");

                // Store fields needed for enrichment later
                tableChunk.put("_isTable", true);
                tableChunk.put("_pageNum", pageNum);
                tableChunk.put("_sectionHeader", sectionHeader);

                chunks.add(tableChunk);

                currentText.setLength(0);
                currentMetadata = new HashMap<>();
                currentPageNumbers.clear();

            } else {
                // Narrative Text
                if (currentText.length() + text.length() + 1 > MAX_CHUNK_SIZE) {
                    finalizeChunk(chunks, currentText, currentMetadata, buildContextString(headerStack),
                            currentPageNumbers);
                }
                currentText.append(text).append("\n");
                if (currentMetadata.isEmpty()) {
                    currentMetadata.putAll(element.getMetadata());
                }
            }
        }

        if (currentText.length() > 0) {
            finalizeChunk(chunks, currentText, currentMetadata, buildContextString(headerStack), currentPageNumbers);
        }

        // Parallel Enrichment Phase
        enrichChunksParallel(chunks);

        return chunks;
    }

    private String buildContextString(Stack<HeaderNode> stack) {
        if (stack.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder("[Context: ");
        for (int i = 0; i < stack.size(); i++) {
            sb.append(stack.get(i).text);
            if (i < stack.size() - 1) {
                sb.append(" > ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private void finalizeChunk(List<Map<String, Object>> chunks, StringBuilder currentText,
            Map<String, Object> currentMetadata, String contextString, Set<String> pageNumbers) {
        String content = currentText.toString().trim();
        if (content.isEmpty())
            return;

        // Format page numbers
        String pageRange = pageNumbers.stream()
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        return -1;
                    }
                })
                .filter(i -> i > 0)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        if (pageRange.isEmpty() && !pageNumbers.isEmpty()) {
            pageRange = String.join(", ", pageNumbers);
        }

        String sectionHeader = contextString.replace("[Context: ", "").replace("]", "");

        // If content is huge, split recursively, BUT prepend context to each split
        if (content.length() > MAX_CHUNK_SIZE) {
            Document doc = Document.from(content);
            DocumentSplitter recursSplitter = DocumentSplitters.recursive(MAX_CHUNK_SIZE, OVERLAP);
            List<TextSegment> subSegments = recursSplitter.split(doc);

            for (TextSegment seg : subSegments) {
                Map<String, Object> chunk = new HashMap<>();
                String chunkContent = contextString + "\n" + seg.text();
                chunk.put("text", chunkContent);
                chunk.put("metadata", new HashMap<>(currentMetadata));
                chunk.put("type", "TEXT_SPLIT");

                chunk.put("_isTable", false);
                chunk.put("_pageNum", pageRange);
                chunk.put("_sectionHeader", sectionHeader);

                chunks.add(chunk);
            }
        } else {
            Map<String, Object> chunk = new HashMap<>();
            String chunkContent = contextString + "\n" + content;
            chunk.put("text", chunkContent);
            chunk.put("type", "TEXT");
            chunk.put("metadata", new HashMap<>(currentMetadata));

            chunk.put("_isTable", false);
            chunk.put("_pageNum", pageRange);
            chunk.put("_sectionHeader", sectionHeader);

            chunks.add(chunk);
        }

        currentText.setLength(0);
        currentMetadata.clear();
        pageNumbers.clear();
    }

    private void enrichChunksParallel(List<Map<String, Object>> chunks) {
        System.out.println("Starting parallel enrichment for " + chunks.size() + " chunks...");

        List<java.util.concurrent.CompletableFuture<Void>> futures = chunks.stream()
                .map(chunk -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                    // Temporarily commenting out unused variables while enrichment is disabled to
                    // avoid lints
                    // String text = (String) chunk.get("text");
                    // boolean isTable = (boolean) chunk.get("_isTable");
                    String pageNum = (String) chunk.get("_pageNum");
                    String sectionHeader = (String) chunk.get("_sectionHeader");

                    ChunkMetadata enrichment = null;
                    try {
                        // AI Enrichment is currently disabled. To re-enable: uncomment variables above
                        // and enrichment call below.
                        // enrichment = enrichmentService.enrichChunk(text, isTable);
                        enrichment = new ChunkMetadata();
                    } catch (Exception e) {
                        System.err.println("Enrichment failed: " + e.getMessage());
                    }

                    if (enrichment == null)
                        enrichment = new ChunkMetadata();
                    enrichment.setPageNumber(pageNum != null ? pageNum : "");
                    enrichment.setSectionHeader(sectionHeader);

                    // Merge enriched metadata
                    @SuppressWarnings("unchecked")
                    Map<String, Object> currentMeta = (Map<String, Object>) chunk.get("metadata");
                    if (currentMeta == null)
                        currentMeta = new HashMap<>();

                    currentMeta.putAll(enrichment.toMap());
                    chunk.put("metadata", currentMeta);

                    // Cleanup temp fields
                    chunk.remove("_isTable");
                    chunk.remove("_pageNum");
                    chunk.remove("_sectionHeader");
                })).collect(Collectors.toList());

        // Wait for all to complete
        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                .join();
        System.out.println("Parallel enrichment completed.");
    }

    public List<String> chunkContent(String content) {
        return new ArrayList<>();
    }

    public void saveChunks(String filename, List<?> chunks) throws IOException {
        Path directory = Paths.get("data/chunks");
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        String savedFilename = filename + "_chunks.json";
        Path file = directory.resolve(savedFilename);

        String jsonLink = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(chunks);
        Files.write(file, jsonLink.getBytes(StandardCharsets.UTF_8));

        System.out.println("Saved chunks to: " + file.toAbsolutePath());
    }
}
