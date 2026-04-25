package com.jawaher.GPT.controllers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.DTO.MultiLLMResponse;
import com.jawaher.GPT.services.MultiLLMService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MultiLLMController {

    private final MultiLLMService multiLLMService;

    public MultiLLMController(MultiLLMService multiLLMService) {
        this.multiLLMService = multiLLMService;
    }

    @PostMapping("/chat/multi")
    public ResponseEntity<MultiLLMResponse> multiChat(@RequestBody ChatRequest chatRequest) {
        return ResponseEntity.ok(multiLLMService.chat(chatRequest));
    }

    @PostMapping("/chat/gemini")
    public ResponseEntity<String> geminiChat(@RequestBody ChatRequest chatRequest) {
        return ResponseEntity.ok(multiLLMService.chatGeminiOnly(chatRequest));
    }
}
