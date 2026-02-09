package com.yin.cita.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
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

    @Value("${langchain4j.pinecone.api-key}")
    private String pineconeApiKey;

    @Value("${langchain4j.pinecone.environment}")
    private String pineconeEnvironment;

    @Value("${langchain4j.pinecone.index-name}")
    private String indexName;

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> embeddingStore;

    @PostConstruct
    public void init() {
        // Initialize Embedding Model (OpenAI text-embedding-3-small)
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(openAiApiKey)
                .modelName("text-embedding-3-small")
                .dimensions(512) // Match existing Pinecone index dimension
                .build();

        // Initialize Pinecone Store
        this.embeddingStore = PineconeEmbeddingStore.builder()
                .apiKey(pineconeApiKey)
                .environment(pineconeEnvironment)
                .index(indexName)
                .build();
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

            // Convert metadata values to strings for Pinecone/LangChain4j compatibility
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

            // Store in Pinecone
            embeddingStore.addAll(embeddings, segments);

            System.out.println("Stored " + segments.size() + " vectors in Pinecone index: " + indexName);
        }
    }

    public List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> findRelevant(String queryText,
            int maxResults) {
        // Embed the query
        Embedding queryEmbedding = embeddingModel.embed(queryText).content();

        // Search in Pinecone (using findRelevant which is fine for now, or use search
        // request builder if available)
        // LangChain4j 0.3x deprecates simple findRelevant for Request object, but
        // simpler to stick for now
        return embeddingStore.findRelevant(queryEmbedding, maxResults);
    }
}
