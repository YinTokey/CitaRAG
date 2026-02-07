
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

        // Let's re-do the loop logic to be safer about context
        chunks.clear();
        headerStack.clear();
        currentText.setLength(0);
        currentMetadata.clear();
        currentPageNumbers.clear();

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
                // Should the Title text itself be part of the chunk text? Yes.
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
                tableChunk.put("metadata", element.getMetadata());
                tableChunk.put("type", "TABLE");

                // ENRICH TABLE
                ChunkMetadata enrichment = null;
                try {
                    enrichment = enrichmentService.enrichChunk(chunkContent, true);
                } catch (Exception e) {
                    System.err.println("Enrichment failed for table: " + e.getMessage());
                }

                if (enrichment == null)
                    enrichment = new ChunkMetadata();
                enrichment.setPageNumber(pageNum != null ? pageNum : "");
                enrichment.setSectionHeader(sectionHeader);

                // Merge enriched metadata into the main metadata
                Map<String, Object> finalMetadata = new HashMap<>(element.getMetadata());
                finalMetadata.putAll(enrichment.toMap());
                tableChunk.put("metadata", finalMetadata);
                // Remove the separate "generated_metadata" field if exists (not adding it
                // anymore)

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
            // Fallback if parsing failed
            pageRange = String.join(", ", pageNumbers);
        }

        // Clean section header (remove [Context: ...])
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

                // ENRICH SPLIT CHUNK
                ChunkMetadata enrichment = null;
                try {
                    enrichment = enrichmentService.enrichChunk(chunkContent, false);
                } catch (Exception e) {
                    System.err.println("Enrichment failed for split chunk: " + e.getMessage());
                }

                if (enrichment == null)
                    enrichment = new ChunkMetadata();
                enrichment.setPageNumber(pageRange);
                enrichment.setSectionHeader(sectionHeader);

                // Merge enriched metadata
                Map<String, Object> finalMetadata = new HashMap<>(currentMetadata);
                finalMetadata.putAll(enrichment.toMap());
                chunk.put("metadata", finalMetadata);

                chunks.add(chunk);
            }
        } else {
            Map<String, Object> chunk = new HashMap<>();

            String chunkContent = contextString + "\n" + content;
            chunk.put("text", chunkContent);
            chunk.put("type", "TEXT");

            // ENRICH STANDARD CHUNK
            ChunkMetadata enrichment = null;
            try {
                enrichment = enrichmentService.enrichChunk(chunkContent, false);
            } catch (Exception e) {
                System.err.println("Enrichment failed for chunk: " + e.getMessage());
            }

            if (enrichment == null)
                enrichment = new ChunkMetadata();
            enrichment.setPageNumber(pageRange);
            enrichment.setSectionHeader(sectionHeader);

            // Merge enriched metadata
            Map<String, Object> finalMetadata = new HashMap<>(currentMetadata);
            finalMetadata.putAll(enrichment.toMap());
            chunk.put("metadata", finalMetadata);

            chunks.add(chunk);
        }

        currentText.setLength(0);
        currentMetadata.clear();
        pageNumbers.clear();
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
