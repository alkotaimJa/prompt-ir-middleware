package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InstructionScaffoldingTransformerTest {

    private final InstructionScaffoldingTransformer transformer =
            new InstructionScaffoldingTransformer("Your task is:", "Please complete this task clearly and accurately.");

    @Test
    void transform_wrapsUserInputWithPrefixAndSuffix() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Explain recursion");

        PromptIR ir = new PromptIR(request.getMessage());
        PromptIR result = transformer.transform(ir, request);

        assertThat(result.getTaskInstruction())
                .isEqualTo("Your task is: Explain recursion Please complete this task clearly and accurately.");
    }

    @Test
    void transform_handlesNullUserInput() {
        ChatRequest request = new ChatRequest();
        PromptIR ir = new PromptIR(null);

        PromptIR result = transformer.transform(ir, request);

        assertThat(result.getTaskInstruction()).contains("Your task is:");
        assertThat(result.getTaskInstruction()).contains("Please complete this task clearly and accurately.");
    }

    @Test
    void transform_usesCustomPrefixAndSuffix() {
        InstructionScaffoldingTransformer custom =
                new InstructionScaffoldingTransformer("BEGIN:", ":END");
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        PromptIR ir = new PromptIR("Hello");

        PromptIR result = custom.transform(ir, request);

        assertThat(result.getTaskInstruction()).isEqualTo("BEGIN: Hello :END");
    }
}
