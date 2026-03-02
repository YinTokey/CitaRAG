package com.yin.cita.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class ChatRequest {

    @Schema(description = "The user's query or prompt", example = "Summarize the uploaded document.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query;

    @Schema(description = "The model to use for generation", example = "llama3", defaultValue = "llama3")
    private String model;

    @Schema(description = "Optional OpenAI API Key provided by the client")
    private String apiKey;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
