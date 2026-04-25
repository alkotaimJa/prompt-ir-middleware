package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleConditioningTransformerTest {

    private static final String DEFAULT_ROLE = "a helpful and precise AI assistant";

    private final RoleConditioningTransformer transformer =
            new RoleConditioningTransformer(DEFAULT_ROLE);

    @Test
    void transform_usesDefaultRoleWhenRequestRoleIsNull() {
        ChatRequest request = new ChatRequest();
        request.setMessage("hi");

        PromptIR ir = transformer.transform(new PromptIR("hi"), request);

        assertThat(ir.getRole()).isEqualTo(DEFAULT_ROLE);
    }

    @Test
    void transform_usesDefaultRoleWhenRequestRoleIsBlank() {
        ChatRequest request = new ChatRequest();
        request.setMessage("hi");
        request.setRole("   ");

        PromptIR ir = transformer.transform(new PromptIR("hi"), request);

        assertThat(ir.getRole()).isEqualTo(DEFAULT_ROLE);
    }

    @Test
    void transform_usesRequestRoleWhenProvided() {
        ChatRequest request = new ChatRequest();
        request.setMessage("hi");
        request.setRole("senior Java engineer");

        PromptIR ir = transformer.transform(new PromptIR("hi"), request);

        assertThat(ir.getRole()).isEqualTo("senior Java engineer");
    }

    @Test
    void transform_copiesOutputFormatFromRequest() {
        ChatRequest request = new ChatRequest();
        request.setMessage("hi");
        request.setOutputFormat("json");

        PromptIR ir = transformer.transform(new PromptIR("hi"), request);

        assertThat(ir.getOutputFormat()).isEqualTo("json");
    }
}
