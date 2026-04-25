package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.compiler.AnthropicPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.GeminiPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.OpenAIPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.ProviderPromptCompiler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderCompilationTransformerTest {

    private final ProviderCompilationTransformer transformer = new ProviderCompilationTransformer(
            List.<ProviderPromptCompiler>of(new OpenAIPromptCompiler(), new AnthropicPromptCompiler(), new GeminiPromptCompiler())
    );

    @Test
    void transform_populatesCompiledPromptsForAllThreeProviders() {
        PromptIR ir = new PromptIR("Tell me about Spring AI");
        ir.setRole("a helpful assistant");
        ir.setTaskInstruction("Your task is: Tell me about Spring AI. Please complete clearly.");
        ir.setContextBlock("");

        PromptIR result = transformer.transform(ir, new ChatRequest());

        assertThat(result.getCompiledPrompts()).containsKeys("openai", "claude", "gemini");
    }

    @Test
    void transform_compiledPromptsHaveNonBlankSystemAndUserText() {
        PromptIR ir = new PromptIR("Hello");
        ir.setRole("expert");
        ir.setTaskInstruction("Do the task.");
        ir.setContextBlock("");

        transformer.transform(ir, new ChatRequest());

        for (CompiledPrompt compiled : ir.getCompiledPrompts().values()) {
            assertThat(compiled.systemText()).isNotBlank();
            assertThat(compiled.userText()).isNotBlank();
        }
    }

    @Test
    void transform_providerNameMatchesMapKey() {
        PromptIR ir = new PromptIR("test");
        ir.setRole("assistant");
        ir.setTaskInstruction("task");
        ir.setContextBlock("");

        transformer.transform(ir, new ChatRequest());

        ir.getCompiledPrompts().forEach((key, compiled) ->
                assertThat(compiled.providerName()).isEqualTo(key));
    }
}
