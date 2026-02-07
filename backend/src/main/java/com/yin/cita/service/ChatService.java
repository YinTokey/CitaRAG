package com.yin.cita.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String openAiApiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    @Autowired
    private VectorStoreService vectorStoreService;

    @PostConstruct
    public void init() {
    }

    public Map<String, Object> chat(String query) {
        // 1. Retrieve relevant chunks (Retrieval only)
        List<EmbeddingMatch<TextSegment>> matches = vectorStoreService.findRelevant(query, 5);

        // 2. Prepare Response with Citations
        Map<String, Object> response = new HashMap<>();

        if (matches.isEmpty()) {
            response.put("answer", "No relevant sections found in the documents for: " + query);
        } else {
            response.put("answer",
                    "Based on your search for \"" + query + "\", I found " + matches.size() + " relevant sections:");
        }

        List<Map<String, Object>> citations = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            Map<String, Object> citation = new HashMap<>();
            citation.put("text", match.embedded().text());
            citation.put("score", match.score());
            citation.put("metadata", match.embedded().metadata().toMap()); // Use toMap() which is the modern API
            citations.add(citation);
        }
        response.put("citations", citations);

        return response;
    }
}
