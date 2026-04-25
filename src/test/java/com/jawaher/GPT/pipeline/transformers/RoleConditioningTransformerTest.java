package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleConditioningTransformerTest {

    private static final String DEFAULT_ROLE = "a helpful and precise AI assistant";

    private final RoleConditioningTransformer transformer =
            new RoleConditioningTransformer(DEFAULT_ROLE);

    // ── Priority 3: default role ──────────────────────────────────────────────

    @Test
    void transform_usesDefaultRoleWhenNoExplicitAndNoDetectedRole() {
        ChatRequest request = new ChatRequest();
        request.setMessage("hi");

        PromptIR ir = transformer.transform(new PromptIR("hi"), request);

        assertThat(ir.getRole()).isEqualTo(DEFAULT_ROLE);
    }

    @Test
    void transform_usesDefaultRoleWhenRequestRoleIsBlankAndNoDetectedRole() {
        ChatRequest request = new ChatRequest();
        request.setMessage("hi");
        request.setRole("   ");

        PromptIR ir = transformer.transform(new PromptIR("hi"), request);

        assertThat(ir.getRole()).isEqualTo(DEFAULT_ROLE);
    }

    // ── Priority 2: detected role ─────────────────────────────────────────────

    @Test
    void transform_usesDetectedRoleWhenNoExplicitRoleProvided() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Write a Python function");

        PromptIR ir = new PromptIR("Write a Python function");
        ir.setDetectedRole("a skilled software developer");

        PromptIR result = transformer.transform(ir, request);

        assertThat(result.getRole()).isEqualTo("a skilled software developer");
    }

    @Test
    void transform_usesDetectedRoleOverDefaultWhenExplicitIsBlank() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Write a Python function");
        request.setRole("   ");

        PromptIR ir = new PromptIR("Write a Python function");
        ir.setDetectedRole("a skilled software developer");

        PromptIR result = transformer.transform(ir, request);

        assertThat(result.getRole()).isEqualTo("a skilled software developer");
    }

    // ── Priority 1: explicit role ─────────────────────────────────────────────

    @Test
    void transform_usesExplicitRoleOverDetectedRole() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Write a Python function");
        request.setRole("pirate");

        PromptIR ir = new PromptIR("Write a Python function");
        ir.setDetectedRole("a skilled software developer");

        PromptIR result = transformer.transform(ir, request);

        assertThat(result.getRole()).isEqualTo("pirate");
    }

    @Test
    void transform_usesExplicitRoleWhenProvided() {
        ChatRequest request = new ChatRequest();
        request.setMessage("hi");
        request.setRole("senior Java engineer");

        PromptIR ir = transformer.transform(new PromptIR("hi"), request);

        assertThat(ir.getRole()).isEqualTo("senior Java engineer");
    }

    // ── Output format ─────────────────────────────────────────────────────────

    @Test
    void transform_copiesOutputFormatFromRequest() {
        ChatRequest request = new ChatRequest();
        request.setMessage("hi");
        request.setOutputFormat("json");

        PromptIR ir = transformer.transform(new PromptIR("hi"), request);

        assertThat(ir.getOutputFormat()).isEqualTo("json");
    }
}

