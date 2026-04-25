package com.jawaher.GPT.services;

import com.jawaher.GPT.DTO.MultiLLMResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class MultiLLMService {

    private static final String GPT_SYSTEM_PROMPT =
            "You are a helpful assistant powered by OpenAI GPT. Provide a clear and concise response.";

    private static final String CLAUDE_SYSTEM_PROMPT =
            "You are Claude, a helpful AI assistant by Anthropic. " +
            "Provide a thoughtful, nuanced, and well-reasoned response.";

    private static final String GEMINI_SYSTEM_PROMPT =
            "You are Gemini, a helpful AI assistant by Google. " +
            "Provide an accurate, informative, and detailed response.";

    private final ChatClient openAiChatClient;
    private final ChatClient claudeChatClient;
    private final ChatClient geminiChatClient;
    private final Executor llmTaskExecutor;

    public MultiLLMService(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient,
            @Qualifier("claudeChatClient") ChatClient claudeChatClient,
            @Qualifier("geminiChatClient") ChatClient geminiChatClient,
            @Qualifier("llmTaskExecutor") Executor llmTaskExecutor) {
        this.openAiChatClient = openAiChatClient;
        this.claudeChatClient = claudeChatClient;
        this.geminiChatClient = geminiChatClient;
        this.llmTaskExecutor = llmTaskExecutor;
    }

    public MultiLLMResponse chat(String message) {
        CompletableFuture<String> gptFuture = CompletableFuture
                .supplyAsync(() -> callGpt(message), llmTaskExecutor)
                .exceptionally(ex -> "GPT error: " + ex.getMessage());
        CompletableFuture<String> claudeFuture = CompletableFuture
                .supplyAsync(() -> callClaude(message), llmTaskExecutor)
                .exceptionally(ex -> "Claude error: " + ex.getMessage());
        CompletableFuture<String> geminiFuture = CompletableFuture
                .supplyAsync(() -> callGemini(message), llmTaskExecutor)
                .exceptionally(ex -> "Gemini error: " + ex.getMessage());

        CompletableFuture.allOf(gptFuture, claudeFuture, geminiFuture).join();

        return new MultiLLMResponse(
                gptFuture.join(),
                claudeFuture.join(),
                geminiFuture.join());
    }

    private String callGpt(String message) {
        return openAiChatClient.prompt()
                .system(GPT_SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();
    }

    private String callClaude(String message) {
        return claudeChatClient.prompt()
                .system(CLAUDE_SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();
    }

    private String callGemini(String message) {
        return geminiChatClient.prompt()
                .system(GEMINI_SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();
    }
}
