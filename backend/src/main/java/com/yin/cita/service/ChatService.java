package com.yin.cita.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Value("${langchain4j.open-ai.chat-model.api-key:demo}")
    private String openAiApiKey;

    @Autowired
    private VectorStoreService vectorStoreService;

    public void streamChat(String query, String modelName, SseEmitter emitter) {
        String finalApiKey = openAiApiKey;

        // 1. Retrieve relevant chunks
        List<EmbeddingMatch<TextSegment>> matches = vectorStoreService.findRelevant(query, 5);

        // 2. Prepare Citations
        List<Map<String, Object>> citations = new ArrayList<>();
        if (!matches.isEmpty()) {
            for (EmbeddingMatch<TextSegment> match : matches) {
                Map<String, Object> citation = new HashMap<>();
                citation.put("text", match.embedded().text());
                citation.put("score", match.score());
                citation.put("metadata", match.embedded().metadata().toMap());
                citations.add(citation);
            }
        }

        // Send Citations Event first
        try {
            Map<String, Object> citationEvent = new HashMap<>();
            citationEvent.put("type", "citations");
            citationEvent.put("data", citations);
            emitter.send(SseEmitter.event().name("citations").data(citationEvent));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return;
        }

        if (matches.isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("token")
                        .data("I couldn't find any relevant information in your documents to answer: " + query));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return;
        }

        String context = matches.stream()
                .map(match -> {
                    Map<String, Object> meta = match.embedded().metadata().toMap();
                    String title = meta.containsKey("title") ? meta.get("title").toString() : "Unknown Title";
                    String author = meta.containsKey("author") ? meta.get("author").toString() : "Unknown Author";
                    return String.format("[Title: %s, Author: %s]\n%s", title, author, match.embedded().text());
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = "You are a helpful research assistant. Use the following context from the user's documents to answer their question.\n\n"
                +
                "CONTEXT:\n" + context + "\n\n" +
                "USER QUESTION: " + query + "\n\n" +
                "INSTRUCTIONS: Provide a concise and accurate answer based ONLY on the provided context. " +
                "If the context doesn't contain the answer, say you don't know.\n" +
                "FORMAT: Use Markdown to structure your response. CRITICAL: Always use a double-newline before starting a list. "
                +
                "Each list item MUST start on a new line with a hyphen and a space (e.g., '\\n\\n- Item 1\\n- Item 2'). "
                +
                "Use bold for key terms and code blocks for code snippets.";

        StreamingChatLanguageModel streamingModel = OpenAiStreamingChatModel.builder()
                .apiKey(finalApiKey)
                .modelName(modelName != null && !modelName.isEmpty() ? modelName : "gpt-5-mini")
                .temperature(1.0)
                .timeout(java.time.Duration.ofSeconds(120))
                .build();

        System.out.println("DEBUG: Generated Prompt (First 500 chars): "
                + (prompt.length() > 500 ? prompt.substring(0, 500) : prompt));

        StringBuilder fullResponse = new StringBuilder();

        streamingModel.generate(prompt, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                fullResponse.append(token);
                try {
                    emitter.send(SseEmitter.event().name("token").data(token));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                System.out.println("DEBUG: Full LLM Response: " + fullResponse.toString());
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("DEBUG: LLM Generation Error: " + error.getMessage());
                emitter.completeWithError(error);
            }
        });
    }
}
