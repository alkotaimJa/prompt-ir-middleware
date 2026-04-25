package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.services.RoleDetectionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleDetectionTransformerTest {

    private final RoleDetectionService detectionService = new RoleDetectionService(true);
    private final RoleDetectionService disabledService  = new RoleDetectionService(false);

    private final RoleDetectionTransformer transformer =
            new RoleDetectionTransformer(detectionService);

    @Test
    void transform_setsDetectedRoleWhenPatternMatches() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Write a Python function to sort a list");

        PromptIR ir = transformer.transform(new PromptIR("Write a Python function to sort a list"), request);

        assertThat(ir.getDetectedRole()).isEqualTo("a skilled software developer");
    }

    @Test
    void transform_leavesDetectedRoleNullWhenNoPatternMatches() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello there");

        PromptIR ir = transformer.transform(new PromptIR("Hello there"), request);

        assertThat(ir.getDetectedRole()).isNull();
    }

    @Test
    void transform_leavesDetectedRoleNullWhenDetectionIsDisabled() {
        RoleDetectionTransformer disabledTransformer = new RoleDetectionTransformer(disabledService);
        ChatRequest request = new ChatRequest();
        request.setMessage("Write a Python function");

        PromptIR ir = disabledTransformer.transform(new PromptIR("Write a Python function"), request);

        assertThat(ir.getDetectedRole()).isNull();
    }

    @Test
    void transform_setsDebuggerRoleForErrorKeyword() {
        ChatRequest request = new ChatRequest();
        request.setMessage("There is an error in my application");

        PromptIR ir = transformer.transform(new PromptIR("There is an error in my application"), request);

        assertThat(ir.getDetectedRole()).isEqualTo("an expert software engineer and debugger");
    }

    @Test
    void transform_returnsIrInstance() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        PromptIR original = new PromptIR("Hello");

        PromptIR returned = transformer.transform(original, request);

        assertThat(returned).isSameAs(original);
    }
}
