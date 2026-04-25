package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextDelimitationTransformerTest {

    private final ContextDelimitationTransformer transformer = new ContextDelimitationTransformer();

    @Test
    void transform_wrapsContextWithDelimiters() {
        ChatRequest request = new ChatRequest();
        request.setContext("Background information about the project.");

        PromptIR ir = transformer.transform(new PromptIR("msg"), request);

        assertThat(ir.getContextBlock())
                .startsWith("### CONTEXT START ###")
                .endsWith("### CONTEXT END ###")
                .contains("Background information about the project.");
    }

    @Test
    void transform_stripsContextBeforeWrapping() {
        ChatRequest request = new ChatRequest();
        request.setContext("  padded context  ");

        PromptIR ir = transformer.transform(new PromptIR("msg"), request);

        assertThat(ir.getContextBlock()).contains("padded context");
        assertThat(ir.getContextBlock()).doesNotContain("  padded context  ");
    }

    @Test
    void transform_leavesContextBlockNullWhenNoContextProvided() {
        ChatRequest request = new ChatRequest();

        PromptIR ir = transformer.transform(new PromptIR("msg"), request);

        assertThat(ir.getContextBlock()).isNull();
    }

    @Test
    void transform_leavesContextBlockNullForBlankContext() {
        ChatRequest request = new ChatRequest();
        request.setContext("   ");

        PromptIR ir = transformer.transform(new PromptIR("msg"), request);

        assertThat(ir.getContextBlock()).isNull();
    }
}
