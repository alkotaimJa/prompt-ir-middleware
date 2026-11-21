package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintExtractionTransformerTest {

    private PromptIR extract(String message) {
        ChatRequest r = new ChatRequest();
        r.setMessage(message);
        return new ConstraintExtractionTransformer().transform(new PromptIR(message), r);
    }

    @Test
    void extractsAudience() {
        assertThat(extract("Explain recursion to a beginner").getAudience()).startsWith("a beginner");
        assertThat(extract("Explain DNS for a non-technical manager").getAudience()).startsWith("a non-technical reader");
        assertThat(extract("Explain gravity to a child").getAudience()).startsWith("a child");
        assertThat(extract("Give an in-depth analysis of GC pauses").getAudience()).startsWith("an expert reader");
        assertThat(extract("Explain recursion").getAudience()).isNull();
    }

    @Test
    void extractsCountsAndMarksExplicitLength() {
        PromptIR ir = extract("Give me three reasons to use TypeScript");
        assertThat(ir.getConstraints()).contains("Provide exactly 3 reasons.");
        assertThat(ir.isExplicitLengthConstraint()).isTrue();
    }

    @Test
    void extractsWordLimitAndBrevity() {
        assertThat(extract("Describe Kafka in under 100 words").getConstraints()).contains("Keep the answer under 100 words.");
        assertThat(extract("Briefly, what is a mutex?").getConstraints()).contains("Keep the answer brief.");
    }

    @Test
    void extractsProgrammingAndResponseLanguage() {
        assertThat(extract("Implement binary search in C++ please").getConstraints()).contains("Use C++.");
        assertThat(extract("Write a parser using python").getConstraints()).contains("Use Python.");
        assertThat(extract("Explain OAuth and answer in Arabic").getConstraints()).contains("Respond in Arabic.");
    }

    @Test
    void noConstraintsForPlainQuestion() {
        PromptIR ir = extract("What is the CAP theorem?");
        assertThat(ir.getConstraints()).isEmpty();
        assertThat(ir.isExplicitLengthConstraint()).isFalse();
    }
}
