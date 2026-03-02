package com.yin.cita.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@CrossOrigin(origins = "http://localhost:5173") // Allow frontend access
public class ModelController {

    @GetMapping
    public List<String> listModels() {
        return List.of("gpt-5-mini", "gpt-5-nano");
    }

}
