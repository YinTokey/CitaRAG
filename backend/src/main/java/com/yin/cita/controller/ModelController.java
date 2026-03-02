package com.yin.cita.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/models")
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend access
public class ModelController {

    @GetMapping
    public List<String> listModels() {
        return List.of("gpt-5-mini", "gpt-5-nano");
    }

    @PostMapping(value = "/pull", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> pullModel(@RequestBody Map<String, String> payload) {
        // Stub for frontend compatibility
        return Flux.just("{\"status\":\"success\"}");
    }
}
