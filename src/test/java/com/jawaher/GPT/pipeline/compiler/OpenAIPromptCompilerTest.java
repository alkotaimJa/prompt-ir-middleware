package com.jawaher.GPT.pipeline.compiler;

import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIPromptCompilerTest {

    private final OpenAIPromptCompiler compiler = new OpenAIPromptCompiler();

    @Test
    void providerName_returnsOpenai() {
        assertThat(compiler.providerName()).isEqualTo("openai");
    }

    @Test
    void compile_systemTextContainsRole() {
        PromptIR ir = buildIr("senior engineer", "Do the task.", "", "What is Spring?");

        CompiledPrompt compiled = compiler.compile(ir);

        assertThat(compiled.systemText()).contains("senior engineer");
        assertThat(compiled.providerName()).isEqualTo("openai");
    }

    @Test
    void compile_userTextContainsInstructionContextAndInput() {
        PromptIR ir = buildIr("assistant", "Your task is: explain.", "### CONTEXT START ###\nBackground.\n### CONTEXT END ###", "Explain it.");

        CompiledPrompt compiled = compiler.compile(ir);

        assertThat(compiled.userText()).contains("Your task is: explain.");
        assertThat(compiled.userText()).contains("Background.");
        assertThat(compiled.userText()).contains("Explain it.");
    }

    @Test
    void compile_userTextOmitsBlankContextBlock() {
        PromptIR ir = buildIr("assistant", "Task instruction.", "", "User input.");

        CompiledPrompt compiled = compiler.compile(ir);

        assertThat(compiled.userText()).doesNotContain("### CONTEXT");
        assertThat(compiled.userText()).contains("Task instruction.");
        assertThat(compiled.userText()).contains("User input.");
    }

    private static PromptIR buildIr(String role, String instruction, String context, String userInput) {
        PromptIR ir = new PromptIR(userInput);
        ir.setRole(role);
        ir.setTaskInstruction(instruction);
        ir.setContextBlock(context);
        return ir;
    }
}
