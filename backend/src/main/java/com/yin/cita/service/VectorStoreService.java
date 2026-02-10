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

            // Warm-up the model to ensure it's loaded before user requests
            warmUpModel();

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void warmUpModel() {
        new Thread(() -> {
            try {
                System.out.println("Warming up embedding model...");
                // Simple retry loop for warmup
                int maxRetries = 5;
                for (int i = 0; i < maxRetries; i++) {
                    try {
                        embeddingModel.embed(dev.langchain4j.data.segment.TextSegment.from("Warm up")).content();
                        System.out.println("Embedding model warmed up and ready.");
                        break;
                    } catch (Exception e) {
                        if (i == maxRetries - 1)
                            throw e;
                        System.err.println("Warmup attempt " + (i + 1) + " failed, retrying in 5s: " + e.getMessage());
                        Thread.sleep(5000);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to warm up model after retries: " + e.getMessage());
            }
        }).start();
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
            int batchSize = 10;
            int totalSegments = allSegments.size();

            System.out.println("Starting embedding generation for " + totalSegments + " segments...");

            List<List<TextSegment>> batches = new ArrayList<>();
            for (int i = 0; i < totalSegments; i += batchSize) {
                batches.add(allSegments.subList(i, Math.min(i + batchSize, totalSegments)));
            }

            // Process batches in parallel
            // Note: Since Ollama on CPU might be the bottleneck, we limit concurrency to
            // avoid thrashing
            // but enough to keep the queue feeding. Common pool is usually sufficient.
            java.util.concurrent.atomic.AtomicInteger processedCount = new java.util.concurrent.atomic.AtomicInteger(0);

            batches.parallelStream().forEach(batch -> {
                try {
                    List<Embedding> embeddings = embeddingModel.embedAll(batch).content();
                    embeddingStore.addAll(embeddings, batch);

                    int current = processedCount.addAndGet(batch.size());
                    System.out.println("Processed " + current + "/" + totalSegments + " segments");
                } catch (Exception e) {
                    System.err.println("Error processing batch: " + e.getMessage());
                    throw new RuntimeException("Failed to process batch", e);
                }
            });

            System.out.println("Stored " + totalSegments + " vectors in Milvus collection: " + collectionName);
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
