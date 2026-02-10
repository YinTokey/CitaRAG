package com.yin.cita.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
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

    @Value("${langchain4j.open-ai.embedding-model.api-key}")
    private String openAiApiKey;

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
            // Initialize Embedding Model (OpenAI text-embedding-3-small)
            this.embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(openAiApiKey)
                    .modelName("text-embedding-3-small")
                    .dimensions(dimension)
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

        List<TextSegment> segments = new ArrayList<>();

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

            segments.add(TextSegment.from(text, metadata));
        }

        if (!segments.isEmpty()) {
            // Generate Embeddings
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

            // Store in Milvus
            embeddingStore.addAll(embeddings, segments);

            System.out.println("Stored " + segments.size() + " vectors in Milvus collection: " + collectionName);
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
