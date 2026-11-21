package com.jawaher.GPT.pipeline.compiler;

import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptCompilerTest {

    private static PromptIR ir() {
        PromptIR ir = new PromptIR("Compare A and B");
        ir.setRole("an experienced software engineer");
        ir.setTaskInstruction("Compare the options in the user's request.");
        ir.getSuccessCriteria().add("Compare the options on each criterion.");
        ir.setAudience("a beginner");
        ir.getConstraints().add("Use Java.");
        ir.setContextBlock("I am a student.");
        ir.setFormatDirective("Present the comparison as a Markdown table, followed by a short verdict.");
        ir.setLengthGuidance("Be proportionate.");
        ir.getResponseGuidelines().add("State assumptions.");
        return ir;
    }

    @Test
    void systemTextHasRoleAndGuidelines() {
        CompiledPrompt p = new OpenAIPromptCompiler().compile(ir());
        assertThat(p.providerName()).isEqualTo("openai");
        assertThat(p.systemText()).startsWith("You are an experienced software engineer.")
                .contains("Response guidelines:\n- Present the comparison")
                .contains("- Be proportionate.").contains("- State assumptions.");
    }

    @Test
    void markdownSectionsInOrderForOpenAiAndGemini() {
        for (CompiledPrompt p : new CompiledPrompt[]{new OpenAIPromptCompiler().compile(ir()), new GeminiPromptCompiler().compile(ir())}) {
            String u = p.userText();
            assertThat(u.indexOf("## Task")).isLessThan(u.indexOf("## Audience and constraints"));
            assertThat(u.indexOf("## Audience and constraints")).isLessThan(u.indexOf("## Context provided by the user"));
            assertThat(u.indexOf("## Context provided by the user")).isLessThan(u.indexOf("## Request"));
            assertThat(u).contains("- Audience: a beginner.").contains("- Use Java.").endsWith("## Request\nCompare A and B");
        }
    }

    @Test
    void claudeUsesXmlTags() {
        String u = new AnthropicPromptCompiler().compile(ir()).userText();
        assertThat(u).startsWith("<task>\n").contains("</task>").contains("<constraints>").contains("<context>\nI am a student.\n</context>")
                .endsWith("<request>\nCompare A and B\n</request>");
        assertThat(u).doesNotContain("##");
    }

    @Test
    void emptyIrCompilesToRawRequest() {
        PromptIR bare = new PromptIR("Just the question");
        bare.setRole("");
        CompiledPrompt p = new AnthropicPromptCompiler().compile(bare);
        assertThat(p.systemText()).isEmpty();
        assertThat(p.userText()).isEqualTo("Just the question");
    }
}
