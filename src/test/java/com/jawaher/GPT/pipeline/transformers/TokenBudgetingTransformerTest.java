package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.model.TaskType;
import com.jawaher.GPT.pipeline.TestPipelines;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBudgetingTransformerTest {

    private PromptIR compiled(String input, String context) {
        PromptIR ir = new PromptIR(input);
        ir.setTaskType(TaskType.GENERAL);
        ir.setTaskInstruction("Respond to the user's request.");
        ir.setContextBlock(context);
        new IRNormalisationTransformer().transform(ir, new ChatRequest());
        new ProviderCompilationTransformer(TestPipelines.compilers()).transform(ir, new ChatRequest());
        return ir;
    }

    @Test
    void trimsOnlyContextAndNeverTheUserRequest() {
        String input = "Q".repeat(200);
        PromptIR ir = compiled(input, "C".repeat(10_000));

        new TokenBudgetingTransformer(500, 500, 500, TestPipelines.compilers()).transform(ir, new ChatRequest());

        assertThat(ir.getContextBlock().length()).isLessThan(10_000);
        assertThat(ir.getUserInput()).isEqualTo(input);
        assertThat(ir.getCompiledPrompts().get("openai").userText()).contains(input);
        assertThat(ir.getProviderHints()).containsKey("contextTruncated");
    }

    @Test
    void leavesPromptUntouchedWithinBudget() {
        PromptIR ir = compiled("Short question", "Short context");
        String before = ir.getCompiledPrompts().get("claude").userText();

        new TokenBudgetingTransformer(128_000, 200_000, 1_000_000, TestPipelines.compilers()).transform(ir, new ChatRequest());

        assertThat(ir.getCompiledPrompts().get("claude").userText()).isEqualTo(before);
        assertThat(ir.getTokenBudget()).isEqualTo(128_000);
    }

    @Test
    void requestMaxTokensLowersBudget() {
        ChatRequest r = new ChatRequest();
        r.setMaxTokens(1000);
        PromptIR ir = compiled("q", "");
        new TokenBudgetingTransformer(128_000, 200_000, 1_000_000, TestPipelines.compilers()).transform(ir, r);
        assertThat(ir.getTokenBudget()).isEqualTo(1000);
    }
}
