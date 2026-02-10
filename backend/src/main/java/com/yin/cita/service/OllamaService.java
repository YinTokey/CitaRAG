package com.yin.cita.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OllamaService {

    @Value("${langchain4j.ollama.embedding-model.base-url:http://127.0.0.1:11434}")
    private String ollamaBaseUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public OllamaService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public List<String> listModels() {
        try {
            String response = webClient.get()
                    .uri(ollamaBaseUrl + "/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            if (response == null)
                return Collections.emptyList();

            OllamaTagsResponse tagsResponse = objectMapper.readValue(response, OllamaTagsResponse.class);
            if (tagsResponse == null || tagsResponse.models == null)
                return Collections.emptyList();

            return tagsResponse.models.stream()
                    .map(m -> m.name)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private final java.util.concurrent.ConcurrentHashMap<String, Flux<String>> activeDownloads = new java.util.concurrent.ConcurrentHashMap<>();

    public Flux<String> pullModel(String modelName) {
        return activeDownloads.computeIfAbsent(modelName, name -> {
            Map<String, Object> request = Map.of("name", name, "stream", true);
            return webClient.post()
                    .uri(ollamaBaseUrl + "/api/pull")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .doFinally(signal -> activeDownloads.remove(name))
                    .share(); // Share the stream with multiple subscribers
        });
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static class OllamaTagsResponse {
        @JsonProperty("models")
        public List<OllamaModel> models;
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static class OllamaModel {
        @JsonProperty("name")
        public String name;
    }
}
