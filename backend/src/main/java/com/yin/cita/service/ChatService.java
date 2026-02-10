package com.yin.cita.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
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

    @Value("${langchain4j.ollama.embedding-model.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private OllamaService ollamaService;

    // Cache models to avoid rebuilding on every request if possible,
    // but building is cheap so we can just build on demand.

    public void streamChat(String query, String modelName, SseEmitter emitter) {
        // 0. Validate that the model exists before attempting inference
        try {
            List<String> availableModels = ollamaService.listModels();
            String actualModelName = modelName != null && !modelName.isEmpty() ? modelName : "phi3:mini";

            // Check if model exists (case-insensitive and handles tags like :latest)
            boolean modelExists = availableModels.stream()
                    .anyMatch(m -> m.toLowerCase().contains(actualModelName.toLowerCase().split(":")[0]));

            if (!modelExists) {
                Map<String, Object> errorEvent = new HashMap<>();
                errorEvent.put("type", "error");
                errorEvent.put("message", "Model '" + actualModelName
                        + "' is not available. Please download it first from the model selector.");
                emitter.send(SseEmitter.event().name("error").data(errorEvent));
                emitter.complete();
                return;
            }
        } catch (Exception e) {
            System.err.println("Failed to check model availability: " + e.getMessage());
            // Continue anyway - model check is best-effort
        }

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

        // 3. Synthesize Answer using Context
        String context = matches.stream()
                .map(match -> match.embedded().text())
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

        // Build Ollama Chat Model on demand with selected model
        StreamingChatLanguageModel streamingModel = dev.langchain4j.model.ollama.OllamaStreamingChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(modelName != null && !modelName.isEmpty() ? modelName : "phi3:mini")
                .timeout(java.time.Duration.ofSeconds(120))
                .temperature(0.7) // Good default
                .build();

        streamingModel.generate(prompt, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                try {
                    emitter.send(SseEmitter.event().name("token").data(token));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                emitter.completeWithError(error);
            }
        });
    }
}
