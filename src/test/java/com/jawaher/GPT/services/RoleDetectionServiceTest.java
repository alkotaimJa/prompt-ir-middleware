package com.jawaher.GPT.services;

import com.jawaher.GPT.model.TaskType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleDetectionServiceTest {

    private final RoleDetectionService service = new RoleDetectionService(true);

    @Test
    void doesNotMisfireOnAmbiguousWords() {
        // Each of these produced a wrong persona with the v1 single-word rules.
        assertThat(service.detectRole("Explain Moore's law and whether it still holds")).isNull();
        assertThat(service.detectRole("What is the margin of error in a survey?")).isNull();
        assertThat(service.detectRole("Explain the class struggle in Marx")).isNull();
        assertThat(service.detectRole("What are the health benefits of walking?")).isNull();
        assertThat(service.detectRole("How do vaccines train the immune system?")).isNull();
    }

    @Test
    void codingTasksGetSoftwareEngineer() {
        assertThat(service.detectRole("Write a palindrome check", TaskType.CODING, null)).isEqualTo("an experienced software engineer");
        assertThat(service.detectRole("Why does this throw an exception?", TaskType.CODING, null))
                .isEqualTo("an expert software engineer experienced in debugging");
    }

    @Test
    void unambiguousDomainTerms() {
        assertThat(service.detectRole("Compare REST and GraphQL scalability")).isEqualTo("an experienced software engineer");
        assertThat(service.detectRole("How do I prevent SQL injection?")).isEqualTo("a cybersecurity specialist");
        assertThat(service.detectRole("Interpret this regression output")).isEqualTo("an experienced data analyst");
        assertThat(service.detectRole("Prove the Pythagorean theorem")).isEqualTo("a mathematics expert");
        assertThat(service.detectRole("Translate this sentence into French")).isEqualTo("a professional translator");
    }

    @Test
    void teacherOnlyForBeginnerOrChildAudience() {
        assertThat(service.detectRole("Explain photosynthesis", TaskType.ANALYTICAL, null)).isNull();
        assertThat(service.detectRole("Explain photosynthesis", TaskType.ANALYTICAL, "a child; use simple words"))
                .isEqualTo("a patient teacher");
    }

    @Test
    void disabledReturnsNull() {
        assertThat(new RoleDetectionService(false).detectRole("Write code", TaskType.CODING, null)).isNull();
    }
}
