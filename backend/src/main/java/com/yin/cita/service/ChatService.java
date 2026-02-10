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
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String openAiApiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.temperature}")
    private Double temperature;

    @Autowired
    private VectorStoreService vectorStoreService;

    private ChatLanguageModel model;

    @PostConstruct
    public void init() {
        if (openAiApiKey == null || openAiApiKey.equals("demo")) {
            System.err.println("WARNING: OPENAI_API_KEY is not set. Chat synthesis will fail.");
        }
        model = OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(modelName)
                .temperature(temperature)
                .build();
    }

    public Map<String, Object> chat(String query) {
        // 1. Retrieve relevant chunks
        List<EmbeddingMatch<TextSegment>> matches = vectorStoreService.findRelevant(query, 5);

        // 2. Prepare Response with Citations
        Map<String, Object> response = new HashMap<>();

        if (matches.isEmpty()) {
            response.put("answer", "I couldn't find any relevant information in your documents to answer: " + query);
            response.put("citations", new ArrayList<>());
            return response;
        }

        // 3. Synthesize Answer using Context
        String context = matches.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = "You are a helpful research assistant. Use the following context from the user's documents to answer their question.\n\n"
                +
                "CONTEXT:\n" + context + "\n\n" +
                "USER QUESTION: " + query + "\n\n" +
                "INSTRUCTIONS: Provide a concise and accurate answer based ONLY on the provided context. " +
                "If the context doesn't contain the answer, say you don't know.";

        String answer;
        try {
            answer = model.generate(prompt);
        } catch (Exception e) {
            System.err.println("Failed to generate AI response: " + e.getMessage());
            answer = "I found " + matches.size()
                    + " relevant sections, but I encountered an error generating a summary. Please check the sources below.";
        }

        response.put("answer", answer);

        List<Map<String, Object>> citations = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            Map<String, Object> citation = new HashMap<>();
            citation.put("text", match.embedded().text());
            citation.put("score", match.score());
            citation.put("metadata", match.embedded().metadata().toMap());
            citations.add(citation);
        }
        response.put("citations", citations);

        return response;
    }
}
