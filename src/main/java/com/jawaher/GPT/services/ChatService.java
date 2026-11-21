package com.jawaher.GPT.services;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptPipeline;
import com.jawaher.GPT.pipeline.compiler.OpenAIPromptCompiler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final PromptPipeline promptPipeline;

    public ChatService(
            @Qualifier("openAiChatClient") ChatClient chatClient,
            PromptPipeline promptPipeline) {
        this.chatClient     = chatClient;
        this.promptPipeline = promptPipeline;
    }

    public String chat(ChatRequest request) {
        PromptIR ir = promptPipeline.process(request);
        CompiledPrompt compiled = ir.getCompiledPrompts().get(OpenAIPromptCompiler.PROVIDER_NAME);
        return MultiLLMService.call(chatClient, compiled);
    }
}
