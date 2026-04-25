package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptIRBuilderTransformerTest {

    private final PromptIRBuilderTransformer transformer = new PromptIRBuilderTransformer();

    @Test
    void transform_defaultsNullFieldsToEmptyString() {
        ChatRequest request = new ChatRequest();
        PromptIR ir = new PromptIR(null);

        PromptIR result = transformer.transform(ir, request);

        assertThat(result.getUserInput()).isEqualTo("");
        assertThat(result.getTaskInstruction()).isNotNull();
        assertThat(result.getRole()).isEqualTo("");
        assertThat(result.getContextBlock()).isEqualTo("");
    }

    @Test
    void transform_doesNotOverwriteAlreadySetFields() {
        ChatRequest request = new ChatRequest();
        PromptIR ir = new PromptIR("original input");
        ir.setTaskInstruction("existing instruction");
        ir.setRole("expert");
        ir.setContextBlock("some context");

        PromptIR result = transformer.transform(ir, request);

        assertThat(result.getTaskInstruction()).isEqualTo("existing instruction");
        assertThat(result.getRole()).isEqualTo("expert");
        assertThat(result.getContextBlock()).isEqualTo("some context");
        assertThat(result.getUserInput()).isEqualTo("original input");
    }

    @Test
    void transform_setsTaskInstructionToUserInputWhenInstructionIsNull() {
        ChatRequest request = new ChatRequest();
        PromptIR ir = new PromptIR("user message");

        PromptIR result = transformer.transform(ir, request);

        assertThat(result.getTaskInstruction()).isEqualTo("user message");
    }
}
