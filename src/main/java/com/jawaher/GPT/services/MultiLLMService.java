package com.jawaher.GPT.services;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.DTO.MultiLLMResponse;
import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptPipeline;
import com.jawaher.GPT.pipeline.compiler.AnthropicPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.GeminiPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.OpenAIPromptCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class MultiLLMService {

    private static final Logger log = LoggerFactory.getLogger(MultiLLMService.class);

    private final ChatClient openAiChatClient;
    private final ChatClient claudeChatClient;
    private final ChatClient geminiChatClient;
    private final Executor llmTaskExecutor;
    private final PromptPipeline promptPipeline;

    public MultiLLMService(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient,
            @Qualifier("claudeChatClient") ChatClient claudeChatClient,
            @Qualifier("geminiChatClient") ChatClient geminiChatClient,
            @Qualifier("llmTaskExecutor") Executor llmTaskExecutor,
            PromptPipeline promptPipeline) {
        this.openAiChatClient = openAiChatClient;
        this.claudeChatClient = claudeChatClient;
        this.geminiChatClient = geminiChatClient;
        this.llmTaskExecutor  = llmTaskExecutor;
        this.promptPipeline   = promptPipeline;
    }

    public MultiLLMResponse chat(ChatRequest request) {
        PromptIR ir = promptPipeline.process(request);

        CompiledPrompt gptCompiled    = ir.getCompiledPrompts().get(OpenAIPromptCompiler.PROVIDER_NAME);
        CompiledPrompt claudeCompiled = ir.getCompiledPrompts().get(AnthropicPromptCompiler.PROVIDER_NAME);
        CompiledPrompt geminiCompiled = ir.getCompiledPrompts().get(GeminiPromptCompiler.PROVIDER_NAME);

        CompletableFuture<String> gptFuture = CompletableFuture
                .supplyAsync(() -> callGpt(gptCompiled), llmTaskExecutor)
                .exceptionally(ex -> { log.error("GPT call failed", ex); return "GPT error: service temporarily unavailable"; });
        CompletableFuture<String> claudeFuture = CompletableFuture
                .supplyAsync(() -> callClaude(claudeCompiled), llmTaskExecutor)
                .exceptionally(ex -> { log.error("Claude call failed", ex); return "Claude error: service temporarily unavailable"; });
        CompletableFuture<String> geminiFuture = CompletableFuture
                .supplyAsync(() -> callGemini(geminiCompiled), llmTaskExecutor)
                .exceptionally(ex -> { log.error("Gemini call failed", ex); return "Gemini error: service temporarily unavailable"; });

        CompletableFuture.allOf(gptFuture, claudeFuture, geminiFuture).join();

        return new MultiLLMResponse(
                gptFuture.join(),
                claudeFuture.join(),
                geminiFuture.join());
    }

    public String chatGeminiOnly(ChatRequest request) {
        try {
            PromptIR ir = promptPipeline.process(request);
            CompiledPrompt geminiCompiled = ir.getCompiledPrompts().get(GeminiPromptCompiler.PROVIDER_NAME);
            return callGemini(geminiCompiled);
        } catch (Exception ex) {
            log.error("Gemini call failed", ex);
            return "Gemini error: service temporarily unavailable";
        }
    }

    private String callGpt(CompiledPrompt compiled) {
        return openAiChatClient.prompt()
                .system(compiled.systemText())
                .user(compiled.userText())
                .call()
                .content();
    }

    private String callClaude(CompiledPrompt compiled) {
        return claudeChatClient.prompt()
                .system(compiled.systemText())
                .user(compiled.userText())
                .call()
                .content();
    }

    private String callGemini(CompiledPrompt compiled) {
        return geminiChatClient.prompt()
                .system(compiled.systemText())
                .user(compiled.userText())
                .call()
                .content();
    }
}
