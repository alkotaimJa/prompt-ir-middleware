package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.model.TaskType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskClassificationTransformerTest {

    private TaskType classify(String message, String category) {
        ChatRequest r = new ChatRequest();
        r.setMessage(message);
        r.setTaskType(category);
        return new TaskClassificationTransformer().transform(new PromptIR(message), r).getTaskType();
    }

    @Test
    void usesParticipantCategoryWhenGiven() {
        assertThat(classify("REST and SOAP", "Comparative reasoning")).isEqualTo(TaskType.COMPARATIVE);
        assertThat(classify("anything", "Coding")).isEqualTo(TaskType.CODING);
        assertThat(classify("anything", "FACTUAL_QA")).isEqualTo(TaskType.FACTUAL_QA);
    }

    @Test
    void infersWhenCategoryMissingOrOther() {
        assertThat(classify("Compare REST and GraphQL scalability", null)).isEqualTo(TaskType.COMPARATIVE);
        assertThat(classify("Write a Python function that sorts a list", "Other")).isEqualTo(TaskType.CODING);
        assertThat(classify("List 5 advantages of Docker", null)).isEqualTo(TaskType.INSTRUCTION_FOLLOWING);
        assertThat(classify("Explain how HTTP caching works", null)).isEqualTo(TaskType.ANALYTICAL);
        assertThat(classify("Who invented the World Wide Web?", null)).isEqualTo(TaskType.FACTUAL_QA);
        assertThat(classify("Tell me something interesting", null)).isEqualTo(TaskType.GENERAL);
    }
}
