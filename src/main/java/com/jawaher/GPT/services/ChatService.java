package com.jawaher.GPT.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String chat(String message) {
        return chatClient.prompt(message).call().content();
    }
}
