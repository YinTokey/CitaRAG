package com.yin.cita.controller;

import com.yin.cita.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "Endpoints for interacting with the RAG Chatbot")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Operation(summary = "Stream Chat Response", description = "Streams the LLM response for a given query using SSE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful stream initialization"),
            @ApiResponse(responseCode = "400", description = "Invalid query provided")
    })
    @PostMapping(produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter chat(
            @RequestBody com.yin.cita.dto.ChatRequest request) {
        String query = request.getQuery();
        String model = request.getModel();
        String apiKey = request.getApiKey();

        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(
                300000L); // 5 minutes timeout

        if (query == null || query.trim().isEmpty()) {
            try {
                emitter.completeWithError(new IllegalArgumentException("Query is required"));
            } catch (Exception e) {
                // ignore
            }
            return emitter;
        }

        chatService.streamChat(query, model, apiKey, emitter);
        return emitter;
    }
}
