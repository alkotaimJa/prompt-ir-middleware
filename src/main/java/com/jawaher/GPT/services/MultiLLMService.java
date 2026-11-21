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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
public class MultiLLMService {

    private static final Logger log = LoggerFactory.getLogger(MultiLLMService.class);

    private final ChatClient openAiChatClient;
    private final ChatClient claudeChatClient;
    private final ChatClient geminiChatClient;
    private final Executor llmTaskExecutor;
    private final PromptPipeline promptPipeline;
    private final long timeoutSeconds;

    public MultiLLMService(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient,
            @Qualifier("claudeChatClient") ChatClient claudeChatClient,
            @Qualifier("geminiChatClient") ChatClient geminiChatClient,
            @Qualifier("llmTaskExecutor") Executor llmTaskExecutor,
            PromptPipeline promptPipeline,
            @Value("${experiment.call-timeout-seconds:120}") long timeoutSeconds) {
        this.openAiChatClient = openAiChatClient;
        this.claudeChatClient = claudeChatClient;
        this.geminiChatClient = geminiChatClient;
        this.llmTaskExecutor  = llmTaskExecutor;
        this.promptPipeline   = promptPipeline;
        this.timeoutSeconds   = timeoutSeconds;
    }

    public MultiLLMResponse chat(ChatRequest request) {
        PromptIR ir = promptPipeline.process(request);

        CompiledPrompt gptCompiled    = ir.getCompiledPrompts().get(OpenAIPromptCompiler.PROVIDER_NAME);
        CompiledPrompt claudeCompiled = ir.getCompiledPrompts().get(AnthropicPromptCompiler.PROVIDER_NAME);
        CompiledPrompt geminiCompiled = ir.getCompiledPrompts().get(GeminiPromptCompiler.PROVIDER_NAME);

        CompletableFuture<String> gptFuture = CompletableFuture
                .supplyAsync(() -> callGpt(gptCompiled), llmTaskExecutor)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(ex -> handleProviderError("GPT", ex));
        CompletableFuture<String> claudeFuture = CompletableFuture
                .supplyAsync(() -> callClaude(claudeCompiled), llmTaskExecutor)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(ex -> handleProviderError("Claude", ex));
        CompletableFuture<String> geminiFuture = CompletableFuture
                .supplyAsync(() -> callGemini(geminiCompiled), llmTaskExecutor)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(ex -> handleProviderError("Gemini", ex));

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
            return handleProviderError("Gemini", ex);
        }
    }

    private String handleProviderError(String providerName, Throwable ex) {
        log.error("{} call failed", providerName, ex);
        return providerName + " error: service temporarily unavailable";
    }

    private String callGpt(CompiledPrompt compiled) {
        return call(openAiChatClient, compiled);
    }

    private String callClaude(CompiledPrompt compiled) {
        return call(claudeChatClient, compiled);
    }

    private String callGemini(CompiledPrompt compiled) {
        return call(geminiChatClient, compiled);
    }

    /** Sends a compiled prompt; the system message is omitted when empty (Spring AI rejects blank text). */
    static String call(ChatClient client, CompiledPrompt compiled) {
        ChatClient.ChatClientRequestSpec spec = client.prompt();
        if (compiled.systemText() != null && !compiled.systemText().isBlank()) {
            spec = spec.system(compiled.systemText());
        }
        return spec.user(compiled.userText()).call().content();
    }
}
