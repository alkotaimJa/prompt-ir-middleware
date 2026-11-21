package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.model.TaskType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for T3, T5, T6, T7, T8 and T9. */
class ContentLayersTest {

    private static PromptIR ir(String msg, TaskType type) {
        PromptIR ir = new PromptIR(msg);
        ir.setTaskType(type);
        return ir;
    }

    private static ChatRequest req(String msg) {
        ChatRequest r = new ChatRequest();
        r.setMessage(msg);
        return r;
    }

    @Test
    void scaffoldingIsTaskSpecificAndHasNoGenericWrapper() {
        for (TaskType t : TaskType.values()) {
            PromptIR out = new InstructionScaffoldingTransformer().transform(ir("x", t), req("x"));
            assertThat(out.getTaskInstruction()).isNotBlank().doesNotContain("Your task is");
            assertThat(out.getSuccessCriteria()).isNotEmpty();
        }
        PromptIR coding = new InstructionScaffoldingTransformer().transform(ir("x", TaskType.CODING), req("x"));
        assertThat(coding.getSuccessCriteria()).contains("Mention important edge cases or limitations.");
    }

    @Test
    void roleConditioningPriorityAndNoDefaultPersona() {
        RoleConditioningTransformer t = new RoleConditioningTransformer("");
        PromptIR detected = ir("x", TaskType.GENERAL);
        detected.setDetectedRole("a data analyst");
        assertThat(t.transform(detected, req("x")).getRole()).isEqualTo("a data analyst");

        ChatRequest explicit = req("x");
        explicit.setRole("a historian");
        assertThat(t.transform(detected, explicit).getRole()).isEqualTo("a historian");

        assertThat(t.transform(ir("x", TaskType.GENERAL), req("x")).getRole()).isEmpty();
        assertThat(new RoleConditioningTransformer("a tutor").transform(ir("x", TaskType.GENERAL), req("x")).getRole())
                .isEqualTo("a tutor");
    }

    @Test
    void outputFormatPriority() {
        OutputFormatTransformer t = new OutputFormatTransformer();
        ChatRequest explicit = req("Compare A and B");
        explicit.setOutputFormat("bullets");
        assertThat(t.transform(ir("Compare A and B", TaskType.COMPARATIVE), explicit).getOutputFormat()).isEqualTo("bullet_points");
        assertThat(t.transform(ir("Return it as JSON", TaskType.GENERAL), req("Return it as JSON")).getOutputFormat()).isEqualTo("json");
        assertThat(t.transform(ir("Compare A and B", TaskType.COMPARATIVE), req("Compare A and B")).getOutputFormat()).isEqualTo("table");
        assertThat(t.transform(ir("Sort a list", TaskType.CODING), req("Sort a list")).getFormatDirective()).contains("fenced code blocks");
        assertThat(t.transform(ir("What is DNS?", TaskType.FACTUAL_QA), req("What is DNS?")).getOutputFormat()).isNull();
    }

    @Test
    void lengthGuardOnlyWithoutExplicitConstraint() {
        LengthControlTransformer t = new LengthControlTransformer("Be proportionate.");
        assertThat(t.transform(ir("x", TaskType.ANALYTICAL), req("x")).getLengthGuidance()).isEqualTo("Be proportionate.");
        PromptIR explicit = ir("x", TaskType.ANALYTICAL);
        explicit.setExplicitLengthConstraint(true);
        assertThat(t.transform(explicit, req("x")).getLengthGuidance()).isNull();
    }

    @Test
    void contextIsStoredWithoutDelimiters() {
        ChatRequest r = req("x");
        r.setContext("  I am a student.  ");
        assertThat(new ContextDelimitationTransformer().transform(ir("x", TaskType.GENERAL), r).getContextBlock())
                .isEqualTo("I am a student.");
        assertThat(new ContextDelimitationTransformer().transform(ir("x", TaskType.GENERAL), req("x")).getContextBlock()).isNull();
    }

    @Test
    void responseGuidelinesDependOnTaskType() {
        ResponseGuidelinesTransformer t = new ResponseGuidelinesTransformer();
        assertThat(t.transform(ir("x", TaskType.COMPARATIVE), req("x")).getResponseGuidelines())
                .contains(ResponseGuidelinesTransformer.PLANNING, ResponseGuidelinesTransformer.UNCERTAINTY);
        assertThat(t.transform(ir("x", TaskType.INSTRUCTION_FOLLOWING), req("x")).getResponseGuidelines())
                .containsExactly(ResponseGuidelinesTransformer.SELF_CHECK);
        assertThat(t.transform(ir("x", TaskType.CODING), req("x")).getResponseGuidelines())
                .doesNotContain(ResponseGuidelinesTransformer.UNCERTAINTY);
    }
}
