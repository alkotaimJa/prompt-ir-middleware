package com.jawaher.GPT.pipeline.compiler;

import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiPromptCompilerTest {

    private final GeminiPromptCompiler compiler = new GeminiPromptCompiler();

    @Test
    void providerName_returnsGemini() {
        assertThat(compiler.providerName()).isEqualTo("gemini");
    }

    @Test
    void compile_systemTextContainsRoleAndGeminiGuidance() {
        PromptIR ir = buildIr("data analyst", "Analyse trends.", "", "Show me the data.");

        CompiledPrompt compiled = compiler.compile(ir);

        assertThat(compiled.systemText()).contains("data analyst");
        assertThat(compiled.systemText()).contains("accurate");
        assertThat(compiled.providerName()).isEqualTo("gemini");
    }

    @Test
    void compile_userTextAssemblesAllSections() {
        PromptIR ir = buildIr("assistant", "Task.", "### CONTEXT START ###\nInfo.\n### CONTEXT END ###", "Question.");

        CompiledPrompt compiled = compiler.compile(ir);

        assertThat(compiled.userText()).contains("Task.");
        assertThat(compiled.userText()).contains("Info.");
        assertThat(compiled.userText()).contains("Question.");
    }

    private static PromptIR buildIr(String role, String instruction, String context, String userInput) {
        PromptIR ir = new PromptIR(userInput);
        ir.setRole(role);
        ir.setTaskInstruction(instruction);
        ir.setContextBlock(context);
        return ir;
    }
}
