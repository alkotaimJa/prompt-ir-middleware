package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class OutputFormatTransformerTest {

    private final OutputFormatTransformer transformer = new OutputFormatTransformer();

    private PromptIR irWithInstruction(String instruction) {
        PromptIR ir = new PromptIR("msg");
        ir.setTaskInstruction(instruction);
        return ir;
    }

    @ParameterizedTest
    @ValueSource(strings = {"bullet_points", "json", "numbered_list", "prose", "sections"})
    void transform_appendsDirectiveForKnownFormats(String format) {
        ChatRequest request = new ChatRequest();
        request.setOutputFormat(format);

        PromptIR ir = transformer.transform(irWithInstruction("Base instruction."), request);

        assertThat(ir.getTaskInstruction())
                .startsWith("Base instruction.")
                .contains(" ");  // appended something
    }

    @Test
    void transform_appendsBulletPointsDirective() {
        ChatRequest request = new ChatRequest();
        request.setOutputFormat("bullet_points");

        PromptIR ir = transformer.transform(irWithInstruction("Do it."), request);

        assertThat(ir.getTaskInstruction()).contains("bulleted list");
    }

    @Test
    void transform_appendsJsonDirective() {
        ChatRequest request = new ChatRequest();
        request.setOutputFormat("json");

        PromptIR ir = transformer.transform(irWithInstruction("Do it."), request);

        assertThat(ir.getTaskInstruction()).contains("valid JSON");
    }

    @Test
    void transform_doesNotModifyInstructionForUnknownFormat() {
        ChatRequest request = new ChatRequest();
        request.setOutputFormat("unknown_format");

        PromptIR ir = transformer.transform(irWithInstruction("Unchanged."), request);

        assertThat(ir.getTaskInstruction()).isEqualTo("Unchanged.");
    }

    @Test
    void transform_doesNotModifyInstructionWhenFormatIsNull() {
        ChatRequest request = new ChatRequest();

        PromptIR ir = transformer.transform(irWithInstruction("Unchanged."), request);

        assertThat(ir.getTaskInstruction()).isEqualTo("Unchanged.");
    }

    @Test
    void transform_detectsJsonFormatFromUserInputWhenRequestFormatMissing() {
        ChatRequest request = new ChatRequest();
        PromptIR ir = new PromptIR("Please return the answer in JSON format.");
        ir.setTaskInstruction("Do it.");

        PromptIR transformed = transformer.transform(ir, request);

        assertThat(transformed.getOutputFormat()).isEqualTo("json");
        assertThat(transformed.getTaskInstruction()).contains("valid JSON");
    }

    @Test
    void transform_detectsBulletPointsFormatFromUserInputWhenRequestFormatMissing() {
        ChatRequest request = new ChatRequest();
        PromptIR ir = new PromptIR("Answer with bullet points only.");
        ir.setTaskInstruction("Do it.");

        PromptIR transformed = transformer.transform(ir, request);

        assertThat(transformed.getOutputFormat()).isEqualTo("bullet_points");
        assertThat(transformed.getTaskInstruction()).contains("bulleted list");
    }

    @Test
    void transform_normalizesKnownFormatAliases() {
        ChatRequest request = new ChatRequest();
        request.setOutputFormat("numbered list");

        PromptIR ir = transformer.transform(irWithInstruction("Do it."), request);

        assertThat(ir.getOutputFormat()).isEqualTo("numbered_list");
        assertThat(ir.getTaskInstruction()).contains("numbered list");
    }

    @Test
    void transform_prioritizesExplicitFormatOverDetectedMessageFormat() {
        ChatRequest request = new ChatRequest();
        request.setOutputFormat("prose");
        PromptIR ir = new PromptIR("Give me JSON output.");
        ir.setTaskInstruction("Do it.");

        PromptIR transformed = transformer.transform(ir, request);

        assertThat(transformed.getOutputFormat()).isEqualTo("prose");
        assertThat(transformed.getTaskInstruction()).contains("flowing prose");
        assertThat(transformed.getTaskInstruction()).doesNotContain("valid JSON");
    }
}
