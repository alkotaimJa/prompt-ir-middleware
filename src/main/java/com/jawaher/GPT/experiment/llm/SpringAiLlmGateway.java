package com.jawaher.GPT.experiment.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Production gateway: dispatches to the Spring AI {@link ChatClient} of each provider.
 */
@Component
@ConditionalOnProperty(name = "experiment.mock-llm", havingValue = "false", matchIfMissing = true)
public class SpringAiLlmGateway implements LlmGateway {

    private final Map<String, ChatClient> clients;
    private final Map<String, String> models;

    public SpringAiLlmGateway(
            @Qualifier("openAiChatClient") ChatClient openAi,
            @Qualifier("claudeChatClient") ChatClient claude,
            @Qualifier("geminiChatClient") ChatClient gemini,
            @Value("${spring.ai.openai.chat.options.model:unknown}") String openAiModel,
            @Value("${spring.ai.anthropic.chat.options.model:unknown}") String claudeModel,
            @Value("${spring.ai.google.genai.chat.options.model:unknown}") String geminiModel) {
        this.clients = Map.of("openai", openAi, "claude", claude, "gemini", gemini);
        this.models = Map.of("openai", openAiModel, "claude", claudeModel, "gemini", geminiModel);
    }

    @Override
    public String complete(String provider, String systemPrompt, String userPrompt) {
        ChatClient client = clients.get(provider);
        if (client == null) {
            throw new IllegalArgumentException("Unknown provider: " + provider);
        }
        ChatClient.ChatClientRequestSpec spec = client.prompt();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            spec = spec.system(systemPrompt);
        }
        String content = spec.user(userPrompt).call().content();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException(provider + " returned an empty response");
        }
        return content;
    }

    @Override
    public String modelFor(String provider) {
        return models.getOrDefault(provider, "unknown");
    }
}
