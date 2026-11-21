package com.jawaher.GPT.pipeline;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.model.TaskType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end tests of the v2 pipeline using real transformer instances (no Spring context). */
class PromptPipelineTest {

    @Test
    void comparativeTask_getsTaskInstructionCriteriaTableAndGuidelines() {
        PromptIR ir = TestPipelines.run("Compare agile and waterfall", "Comparative reasoning", null);

        assertThat(ir.getTaskType()).isEqualTo(TaskType.COMPARATIVE);
        assertThat(ir.getTaskInstruction()).isEqualTo("Compare the options in the user's request.");
        assertThat(ir.getSuccessCriteria()).hasSize(3);
        assertThat(ir.getOutputFormat()).isEqualTo("table");
        assertThat(ir.getLengthGuidance()).isEqualTo(TestPipelines.LENGTH_GUIDANCE);
        CompiledPrompt openai = ir.getCompiledPrompts().get("openai");
        assertThat(openai.systemText()).contains("Markdown table");
        assertThat(openai.userText()).contains("## Task").contains("## Request").endsWith("Compare agile and waterfall");
    }

    @Test
    void userInputIsNeverModifiedAndAppearsExactlyOnce() {
        String msg = "What is the difference between REST and SOAP?";
        PromptIR ir = TestPipelines.run(msg, "Factual QA", null);

        assertThat(ir.getUserInput()).isEqualTo(msg);
        for (CompiledPrompt p : ir.getCompiledPrompts().values()) {
            String u = p.userText();
            assertThat(u.indexOf(msg)).isEqualTo(u.lastIndexOf(msg));
            assertThat(u).contains(msg);
        }
    }

    @Test
    void noGenericSuffixOrDefaultPersona() {
        PromptIR ir = TestPipelines.run("Explain the class struggle in Marx", null, null);

        assertThat(ir.getRole()).isEmpty();
        for (CompiledPrompt p : ir.getCompiledPrompts().values()) {
            assertThat(p.systemText()).doesNotContain("You are").doesNotContain("helpful and precise");
            assertThat(p.userText()).doesNotContain("Please complete this task clearly and accurately");
        }
    }

    @Test
    void providersReceiveIdenticalWording() {
        PromptIR ir = TestPipelines.run("Explain REST APIs to a beginner", "Analytical explanation", "I am a student.");
        CompiledPrompt o = ir.getCompiledPrompts().get("openai");
        CompiledPrompt c = ir.getCompiledPrompts().get("claude");
        CompiledPrompt g = ir.getCompiledPrompts().get("gemini");

        assertThat(o.systemText()).isEqualTo(c.systemText()).isEqualTo(g.systemText());
        assertThat(o.userText()).isEqualTo(g.userText());
        // Claude differs only in delimiters: removing them yields the same words.
        String claudeWords = c.userText().replaceAll("</?[a-z]+>\\n?", "").replaceAll("\\s+", " ").trim();
        String markdownWords = o.userText().replaceAll("## [^\\n]+\\n", "").replaceAll("\\s+", " ").trim();
        assertThat(claudeWords).isEqualTo(markdownWords);
    }

    @Test
    void selectedContextIsDelimitedInItsOwnSection() {
        PromptIR ir = TestPipelines.run("Compare agile and waterfall", "Comparative reasoning",
                "I lead a 5-person startup team.");

        assertThat(ir.getContextBlock()).isEqualTo("I lead a 5-person startup team.");
        assertThat(ir.getCompiledPrompts().get("claude").userText())
                .contains("<context>\nI lead a 5-person startup team.\n</context>");
        assertThat(ir.getCompiledPrompts().get("openai").userText())
                .contains("## Context provided by the user\nI lead a 5-person startup team.");
    }

    @Test
    void explicitCountDisablesLengthGuardAndIsRestated() {
        PromptIR ir = TestPipelines.run("List advantages of REST using 5 bullet points", "Instruction following", null);

        assertThat(ir.getConstraints()).contains("Provide exactly 5 bullet points.");
        assertThat(ir.getLengthGuidance()).isEmpty();
        assertThat(ir.getOutputFormat()).isEqualTo("bullet_points");
        assertThat(ir.getCompiledPrompts().get("openai").userText()).contains("Provide exactly 5 bullet points.");
    }

    @Test
    void disablingAllContentLayersYieldsTheRawPrompt() {
        PromptPipeline none = TestPipelines.pipeline(TestPipelines.ALL_CONTENT_LAYERS);
        ChatRequest r = new ChatRequest();
        r.setMessage("Compare agile and waterfall");
        r.setTaskType("Comparative reasoning");

        PromptIR ir = none.process(r);

        for (CompiledPrompt p : ir.getCompiledPrompts().values()) {
            assertThat(p.systemText()).isEmpty();
            assertThat(p.userText()).isEqualTo("Compare agile and waterfall");
        }
        assertThat(ir.getAppliedLayers()).containsExactly("ir-normalisation", "provider-compilation", "token-budgeting");
    }

    @Test
    void disabledLayersAreSkippedAndRecordedInVersionTag() {
        PromptPipeline p = TestPipelines.pipeline("role-detection, length-control");
        ChatRequest r = new ChatRequest();
        r.setMessage("Write a Java method that reverses a list");
        r.setTaskType("Coding");

        PromptIR ir = p.process(r);

        assertThat(ir.getRole()).isEmpty();
        assertThat(ir.getLengthGuidance()).isEmpty();
        assertThat(ir.getAppliedLayers()).doesNotContain("role-detection", "length-control");
        assertThat(p.versionTag()).isEqualTo(PromptPipeline.VERSION + "-without:length-control,role-detection");
        assertThat(TestPipelines.pipeline("").versionTag()).isEqualTo(PromptPipeline.VERSION);
    }

    @Test
    void infrastructureStagesCannotBeDisabled() {
        PromptIR ir = TestPipelines.pipeline("provider-compilation,ir-normalisation").process(request("Hello"));

        assertThat(ir.getCompiledPrompts()).containsKeys("openai", "claude", "gemini");
    }

    @Test
    void explicitRoleAndFormatFromRequestTakePriority() {
        ChatRequest r = request("Review this design");
        r.setRole("senior code reviewer");
        r.setOutputFormat("numbered_list");

        PromptIR ir = TestPipelines.pipeline("").process(r);

        assertThat(ir.getRole()).isEqualTo("senior code reviewer");
        assertThat(ir.getOutputFormat()).isEqualTo("numbered_list");
        assertThat(ir.getCompiledPrompts().get("openai").systemText())
                .startsWith("You are senior code reviewer.").contains("numbered list");
    }

    @Test
    void tokenBudgetIsMinimumProviderBudget() {
        assertThat(TestPipelines.pipeline("").process(request("Hello")).getTokenBudget()).isEqualTo(128_000);
    }

    private static ChatRequest request(String msg) {
        ChatRequest r = new ChatRequest();
        r.setMessage(msg);
        return r;
    }
}
