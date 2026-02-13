package com.yin.cita.controller;

import com.yin.cita.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping(produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter chat(
            @RequestBody Map<String, Object> payload) {
        String query = (String) payload.get("query");
        String model = (String) payload.get("model");

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

        chatService.streamChat(query, model, emitter);
        return emitter;
    }
}
