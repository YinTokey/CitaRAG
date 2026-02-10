package com.yin.cita.controller;

import com.yin.cita.service.OllamaService;
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

    @Autowired
    private OllamaService ollamaService;

    @GetMapping
    public List<String> listModels() {
        return ollamaService.listModels();
    }

    @PostMapping(value = "/pull", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> pullModel(@RequestBody Map<String, String> payload) {
        String modelName = payload.get("name");
        if (modelName == null || modelName.isEmpty()) {
            return Flux.error(new IllegalArgumentException("Model name is required"));
        }
        return ollamaService.pullModel(modelName);
    }
}
