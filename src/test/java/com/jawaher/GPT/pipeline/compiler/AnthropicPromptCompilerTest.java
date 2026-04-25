package com.jawaher.GPT.pipeline.compiler;

import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicPromptCompilerTest {

    private final AnthropicPromptCompiler compiler = new AnthropicPromptCompiler();

    @Test
    void providerName_returnsClaude() {
        assertThat(compiler.providerName()).isEqualTo("claude");
    }

    @Test
    void compile_systemTextContainsRoleAndAnthropicGuidance() {
        PromptIR ir = buildIr("expert reviewer", "Analyse this.", "", "Some text.");

        CompiledPrompt compiled = compiler.compile(ir);

        assertThat(compiled.systemText()).contains("expert reviewer");
        assertThat(compiled.systemText()).contains("thoughtful");
        assertThat(compiled.providerName()).isEqualTo("claude");
    }

    @Test
    void compile_userTextAssemblesAllSections() {
        PromptIR ir = buildIr("assistant", "Task.", "### CONTEXT START ###\nCtx.\n### CONTEXT END ###", "Input.");

        CompiledPrompt compiled = compiler.compile(ir);

        assertThat(compiled.userText()).contains("Task.");
        assertThat(compiled.userText()).contains("Ctx.");
        assertThat(compiled.userText()).contains("Input.");
    }

    private static PromptIR buildIr(String role, String instruction, String context, String userInput) {
        PromptIR ir = new PromptIR(userInput);
        ir.setRole(role);
        ir.setTaskInstruction(instruction);
        ir.setContextBlock(context);
        return ir;
    }
}
