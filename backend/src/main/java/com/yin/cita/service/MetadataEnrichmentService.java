
package com.yin.cita.service;

import com.yin.cita.model.ChunkMetadata;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class MetadataEnrichmentService {

    @Value("${langchain4j.open-ai.chat-model.api-key:demo}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-5-mini}")
    private String modelName;

    private EnrichmentAgent enrichmentAgent;

    interface EnrichmentAgent {
        @UserMessage("Analyze the following document chunk and generate structured metadata for summarization and retrieval optimization.\n"
                +
                "Is Table: {{isTable}}\n\n" +
                "Chunk Content:\n" +
                "---\n" +
                "{{content}}\n" +
                "---\n")
        ChunkMetadata enrich(@V("content") String content, @V("isTable") boolean isTable);
    }

    @PostConstruct
    public void init() {
        // Use GPT-5 mini as requested
        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(1.0)
                .build();

        this.enrichmentAgent = AiServices.create(EnrichmentAgent.class, model);
    }

    public ChunkMetadata enrichChunk(String content, boolean isTable) {
        // Truncate if too long to save tokens
        String truncatedContent = content.length() > 3000 ? content.substring(0, 3000) + "...[truncated]" : content;
        try {
            return enrichmentAgent.enrich(truncatedContent, isTable);
        } catch (Exception e) {
            System.err.println("Error enriching chunk: " + e.getMessage());
            return null; // Graceful degradation
        }
    }
}
