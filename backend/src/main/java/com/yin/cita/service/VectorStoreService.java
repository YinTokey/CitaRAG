package com.yin.cita.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class VectorStoreService {

    @Value("${langchain4j.ollama.embedding-model.base-url}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.embedding-model.model-name}")
    private String ollamaModelName;

    @Value("${langchain4j.ollama.embedding-model.timeout}")
    private java.time.Duration timeout;

    @Value("${langchain4j.milvus.host}")
    private String milvusHost;

    @Value("${langchain4j.milvus.port}")
    private int milvusPort;

    @Value("${langchain4j.milvus.collection-name}")
    private String collectionName;

    @Value("${langchain4j.milvus.dimension}")
    private int dimension;

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> embeddingStore;

    @PostConstruct
    public void init() {
        try {
            // Initialize Embedding Model (Ollama)
            this.embeddingModel = OllamaEmbeddingModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(ollamaModelName)
                    .timeout(timeout)
                    .build();

            // Initialize Milvus Store
            this.embeddingStore = MilvusEmbeddingStore.builder()
                    .host(milvusHost)
                    .port(milvusPort)
                    .collectionName(collectionName)
                    .dimension(dimension)
                    .retrieveEmbeddingsOnSearch(true) // Helper to retrieve embeddings if needed
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void storeChunks(List<Map<String, Object>> chunks) {
        if (chunks == null || chunks.isEmpty())
            return;

        List<TextSegment> allSegments = new ArrayList<>();

        for (Map<String, Object> chunk : chunks) {
            String text = (String) chunk.get("text");
            if (text == null || text.isEmpty())
                continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> metadataMap = (Map<String, Object>) chunk.get("metadata"); // Flattened metadata

            // Convert metadata values to strings for Milvus/LangChain4j compatibility
            dev.langchain4j.data.document.Metadata metadata = new dev.langchain4j.data.document.Metadata();

            if (metadataMap != null) {
                for (Map.Entry<String, Object> entry : metadataMap.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null) {
                        // Handle list values cleanly (e.g. keywords)
                        if (value instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> list = (List<String>) value;
                            metadata.put(key, String.join(", ", list));
                        } else {
                            metadata.put(key, value.toString());
                        }
                    }
                }
            }

            allSegments.add(TextSegment.from(text, metadata));
        }

        if (!allSegments.isEmpty()) {
            int batchSize = 10; // Process in batches of 10 to avoid timeouts
            int totalSegments = allSegments.size();
            int processed = 0;

            System.out.println("Starting embedding generation for " + totalSegments + " segments...");

            for (int i = 0; i < totalSegments; i += batchSize) {
                int end = Math.min(i + batchSize, totalSegments);
                List<TextSegment> batch = allSegments.subList(i, end);

                try {
                    // Generate Embeddings for batch
                    List<Embedding> embeddings = embeddingModel.embedAll(batch).content();

                    // Store batch in Milvus
                    embeddingStore.addAll(embeddings, batch);

                    processed += batch.size();
                    System.out.println("Processed batch " + (i / batchSize + 1) + "/"
                            + ((totalSegments + batchSize - 1) / batchSize) +
                            " (" + processed + "/" + totalSegments + " segments)");

                } catch (Exception e) {
                    System.err.println("Error processing batch " + (i / batchSize + 1) + ": " + e.getMessage());
                    // Consider whether to throw or continue. For now, we log and rethrow to stop
                    // processing causing partial failure state
                    // or we could continue to try best effort.
                    // Given the user wants to avoid timeouts, failing fast on a batch is better
                    // than hanging.
                    throw new RuntimeException("Failed to process batch of embeddings", e);
                }
            }

            System.out.println("Stored " + processed + " vectors in Milvus collection: " + collectionName);
        }
    }

    public List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> findRelevant(String queryText,
            int maxResults) {
        // Embed the query
        Embedding queryEmbedding = embeddingModel.embed(queryText).content();

        // Search in Milvus
        return embeddingStore.findRelevant(queryEmbedding, maxResults);
    }
}
