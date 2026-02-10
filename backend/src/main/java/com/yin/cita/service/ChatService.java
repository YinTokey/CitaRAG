package com.yin.cita.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

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

    private StreamingChatLanguageModel streamingModel;

    @PostConstruct
    public void init() {
        if (openAiApiKey == null || openAiApiKey.equals("demo")) {
            System.err.println("WARNING: OPENAI_API_KEY is not set. Chat synthesis will fail.");
        }
        streamingModel = OpenAiStreamingChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(modelName)
                .temperature(temperature)
                .build();
    }

    public void streamChat(String query, SseEmitter emitter) {
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

    // Keep the old blocking method just in case, or remove it. For now, removing to
    // force usage of stream or renaming it.
    // Actually, let's keep it but rename it or just let it be if not used.
    // I will remove it to clean up as per requirement "use the stream style".
}
