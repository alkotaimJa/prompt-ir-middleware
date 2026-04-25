package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.compiler.AnthropicPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.GeminiPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.OpenAIPromptCompiler;
import com.jawaher.GPT.pipeline.compiler.ProviderPromptCompiler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBudgetingTransformerTest {

    private static List<ProviderPromptCompiler> allCompilers() {
        return List.of(new OpenAIPromptCompiler(), new AnthropicPromptCompiler(), new GeminiPromptCompiler());
    }

    private TokenBudgetingTransformer transformer(int openAi, int claude, int gemini) {
        return new TokenBudgetingTransformer(openAi, claude, gemini, allCompilers());
    }

    private PromptIR buildIrWithCompiledPrompts(String instruction, String context) {
        PromptIR ir = new PromptIR("user input");
        ir.setRole("assistant");
        ir.setTaskInstruction(instruction);
        ir.setContextBlock(context != null ? context : "");

        new ProviderCompilationTransformer(allCompilers()).transform(ir, new ChatRequest());
        return ir;
    }

    @Test
    void transform_setsTokenBudgetToMinProviderBudget() {
        PromptIR ir = buildIrWithCompiledPrompts("Short task.", null);
        TokenBudgetingTransformer t = transformer(128_000, 200_000, 1_000_000);

        t.transform(ir, new ChatRequest());

        assertThat(ir.getTokenBudget()).isEqualTo(128_000);
    }

    @Test
    void transform_respectsPerRequestMaxTokensHint() {
        PromptIR ir = buildIrWithCompiledPrompts("Short task.", null);
        ChatRequest request = new ChatRequest();
        request.setMaxTokens(500);

        transformer(128_000, 200_000, 1_000_000).transform(ir, request);

        assertThat(ir.getTokenBudget()).isEqualTo(500);
    }

    @Test
    void transform_truncatesContextBlockWhenOverBudget() {
        // Make a huge context that will exceed a tiny budget
        String hugeContext = "X".repeat(10_000);
        PromptIR ir = buildIrWithCompiledPrompts("task", hugeContext);

        // Budget of 100 tokens = 400 chars — much smaller than the content
        transformer(100, 100, 100).transform(ir, new ChatRequest());

        // Context block should have been trimmed
        assertThat(ir.getContextBlock().length()).isLessThan(hugeContext.length());
    }

    @Test
    void transform_doesNotTruncateWhenWithinBudget() {
        String instruction = "Short instruction.";
        PromptIR ir = buildIrWithCompiledPrompts(instruction, null);
        String originalInstruction = ir.getTaskInstruction();

        transformer(128_000, 200_000, 1_000_000).transform(ir, new ChatRequest());

        assertThat(ir.getTaskInstruction()).isEqualTo(originalInstruction);
    }

    @Test
    void transform_recompilesProvidersAfterTruncation() {
        String hugeContext = "C".repeat(10_000);
        PromptIR ir = buildIrWithCompiledPrompts("task", hugeContext);
        String originalUserText = ir.getCompiledPrompts().get("openai").userText();

        transformer(100, 100, 100).transform(ir, new ChatRequest());

        String truncatedUserText = ir.getCompiledPrompts().get("openai").userText();
        assertThat(truncatedUserText.length()).isLessThan(originalUserText.length());
    }

    @Test
    void estimateTokens_returnsLengthDividedByFour() {
        assertThat(TokenBudgetingTransformer.estimateTokens("1234")).isEqualTo(1);
        assertThat(TokenBudgetingTransformer.estimateTokens("12345678")).isEqualTo(2);
        assertThat(TokenBudgetingTransformer.estimateTokens(null)).isEqualTo(0);
    }
}
