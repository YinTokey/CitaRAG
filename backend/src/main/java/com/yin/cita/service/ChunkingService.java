
package com.yin.cita.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yin.cita.model.DocumentElement;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

@Service
public class ChunkingService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_CHUNK_SIZE = 2000;
    private static final int OVERLAP = 200;

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

        // Stack to track header hierarchy (H1 -> H2 -> H3)
        Stack<HeaderNode> headerStack = new Stack<>();

        // Let's re-do the loop logic to be safer about context
        chunks.clear();
        headerStack.clear();
        currentText.setLength(0);
        currentMetadata.clear();

        for (DocumentElement element : elements) {
            String text = element.getText();
            DocumentElement.Type type = element.getType();

            if (type == DocumentElement.Type.TITLE) {
                // A new title means the previous section is done.
                if (currentText.length() > 0) {
                    finalizeChunk(chunks, currentText, currentMetadata, buildContextString(headerStack));
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
                    finalizeChunk(chunks, currentText, currentMetadata, buildContextString(headerStack));
                }

                // Create atomic table chunk WITH context
                Map<String, Object> tableChunk = new HashMap<>();
                String context = buildContextString(headerStack);
                // Inject context into text for searchability
                tableChunk.put("text", context + "\n" + text);
                tableChunk.put("metadata", element.getMetadata());
                tableChunk.put("type", "TABLE");
                chunks.add(tableChunk);

                currentText.setLength(0);
                currentMetadata = new HashMap<>();

            } else {
                // Narrative Text
                if (currentText.length() + text.length() + 1 > MAX_CHUNK_SIZE) {
                    finalizeChunk(chunks, currentText, currentMetadata, buildContextString(headerStack));
                }
                currentText.append(text).append("\n");
                if (currentMetadata.isEmpty()) {
                    currentMetadata.putAll(element.getMetadata());
                }
            }
        }

        if (currentText.length() > 0) {
            finalizeChunk(chunks, currentText, currentMetadata, buildContextString(headerStack));
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
            Map<String, Object> currentMetadata, String contextString) {
        String content = currentText.toString().trim();
        if (content.isEmpty())
            return;

        // If content is huge, split recursively, BUT prepend context to each split
        if (content.length() > MAX_CHUNK_SIZE) {
            Document doc = Document.from(content);
            DocumentSplitter recursSplitter = DocumentSplitters.recursive(MAX_CHUNK_SIZE, OVERLAP);
            List<TextSegment> subSegments = recursSplitter.split(doc);
            for (TextSegment seg : subSegments) {
                Map<String, Object> chunk = new HashMap<>();
                // Context + Segment
                // Note: If context is huge, this might exceed limit again, but usually titles
                // are short.
                chunk.put("text", contextString + "\n" + seg.text());
                chunk.put("metadata", new HashMap<>(currentMetadata));
                chunk.put("type", "TEXT_SPLIT");
                chunks.add(chunk);
            }
        } else {
            Map<String, Object> chunk = new HashMap<>();
            // Inject context if not already present (Chunk 1 might have it if it includes
            // the header, but standardizing is safer)
            // Actually, if we just blindly prepend, valid title might appear twice (once in
            // context, once in text).
            // But redundancy is better than loss.
            // Cleaner: Check if text already starts with context? No, text starts with
            // Header. Context is Path.
            // Example: "H1 > H2" vs Text "H2 \n content".
            // Let's just prepend. The LLM won't mind.

            chunk.put("text", contextString + "\n" + content);
            chunk.put("metadata", new HashMap<>(currentMetadata));
            chunk.put("type", "TEXT");
            chunks.add(chunk);
        }

        currentText.setLength(0);
        currentMetadata.clear();
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
