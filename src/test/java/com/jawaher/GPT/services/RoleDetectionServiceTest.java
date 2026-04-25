package com.jawaher.GPT.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RoleDetectionServiceTest {

    private final RoleDetectionService service = new RoleDetectionService(true);
    private final RoleDetectionService disabledService = new RoleDetectionService(false);

    // ── Debugging / error investigation ──────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "Please debug this code",
            "There is an error in my program",
            "I keep getting an exception in Java",
            "Here is the stack trace",
            "I see a traceback in the output"
    })
    void detectRole_returnsDebuggerForDebuggingInputs(String input) {
        assertThat(service.detectRole(input))
                .isEqualTo("an expert software engineer and debugger");
    }

    // ── Software development ──────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "Write a Python function to sort a list",
            "Implement a binary search algorithm",
            "Create a script to rename files",
            "Refactor this class",
            "Give me a code snippet for HTTP requests"
    })
    void detectRole_returnsDeveloperForCodingInputs(String input) {
        assertThat(service.detectRole(input))
                .isEqualTo("a skilled software developer");
    }

    // ── Data analysis ─────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "Analyze this dataset",
            "Show me the statistics",
            "Draw a chart from this CSV",
            "Visualize the sales data",
            "Calculate the regression"
    })
    void detectRole_returnsDataAnalystForDataInputs(String input) {
        assertThat(service.detectRole(input))
                .isEqualTo("an expert data analyst");
    }

    // ── Translation ───────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "Translate this sentence",
            "How do you say hello in French",
            "Convert this text to Spanish",
            "Write this in German"
    })
    void detectRole_returnsTranslatorForTranslationInputs(String input) {
        assertThat(service.detectRole(input))
                .isEqualTo("a professional translator");
    }

    // ── Creative writing ──────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "Write a poem about autumn",
            "Tell me a short story",
            "Give me a creative narrative",
            "Write fiction about space",
            "Compose a haiku"
    })
    void detectRole_returnsCreativeWriterForCreativeInputs(String input) {
        assertThat(service.detectRole(input))
                .isEqualTo("a creative writer");
    }

    // ── Medical ───────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "What are the symptoms of diabetes",
            "Explain this medical diagnosis",
            "What treatment is available for hypertension",
            "Is this medication safe"
    })
    void detectRole_returnsMedicalAssistantForMedicalInputs(String input) {
        assertThat(service.detectRole(input))
                .isEqualTo("a knowledgeable medical information assistant");
    }

    // ── Legal ─────────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "Explain this contract clause",
            "What does the law say about this",
            "Check this regulation",
            "Is there legal compliance required here",
            "What is my liability in this case"
    })
    void detectRole_returnsLegalAssistantForLegalInputs(String input) {
        assertThat(service.detectRole(input))
                .isEqualTo("a legal information assistant");
    }

    // ── Security ──────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "How do I fix this security vulnerability",
            "Explain SQL injection exploit",
            "What is penetration testing",
            "Describe OWASP top ten risks"
    })
    void detectRole_returnsSecurityExpertForSecurityInputs(String input) {
        assertThat(service.detectRole(input))
                .isEqualTo("a cybersecurity expert");
    }

    // ── Mathematics ───────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "Solve this math equation",
            "What is the derivative of sin(x)",
            "Prove this theorem",
            "Explain Euclidean geometry",
            "Compute this integral"
    })
    void detectRole_returnsMathematicianForMathInputs(String input) {
        assertThat(service.detectRole(input))
                .isEqualTo("an expert mathematician");
    }

    // ── Teaching / explanation ────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "Explain how photosynthesis works",
            "Teach me about recursion",
            "Give me a step-by-step tutorial",
            "What is machine learning",
            "How do neural networks work"
    })
    void detectRole_returnsTeacherForExplanationInputs(String input) {
        assertThat(service.detectRole(input))
                .isEqualTo("a clear and patient teacher");
    }

    // ── No match ──────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "Hello",
            "What should I have for lunch",
            "Tell me a joke",
            "Good morning"
    })
    void detectRole_returnsNullForUnrelatedInputs(String input) {
        assertThat(service.detectRole(input)).isNull();
    }

    @Test
    void detectRole_returnsNullForNullInput() {
        assertThat(service.detectRole(null)).isNull();
    }

    @Test
    void detectRole_returnsNullForBlankInput() {
        assertThat(service.detectRole("   ")).isNull();
    }

    // ── Detection disabled ────────────────────────────────────────────────────

    @Test
    void detectRole_returnsNullWhenDetectionIsDisabled() {
        assertThat(disabledService.detectRole("Write a Python function")).isNull();
    }

    // ── Explicit vs detected ordering: debugging before developer ────────────

    @Test
    void detectRole_prefersDebuggerOverDeveloperWhenBothCouldMatch() {
        // "debug" appears alongside "function" — debugger pattern must win
        assertThat(service.detectRole("debug this function"))
                .isEqualTo("an expert software engineer and debugger");
    }
}
