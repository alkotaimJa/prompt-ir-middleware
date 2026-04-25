package com.jawaher.GPT.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects an appropriate LLM persona from raw user input using ordered regex pattern matching.
 * <p>
 * Each entry maps a compiled {@link Pattern} to a role string. Patterns are tested
 * case-insensitively in declaration order; the first match wins. When no pattern
 * matches, or when detection is disabled via {@code pipeline.role.detection.enabled},
 * {@code null} is returned and the pipeline falls back to the explicit or default role.
 */
@Component
public class RoleDetectionService {

    private final boolean detectionEnabled;

    private record RolePattern(Pattern pattern, String role) {}

    private static final List<RolePattern> PATTERNS = List.of(
            // Debugging / error investigation — tested before generic "write code" patterns
            new RolePattern(
                    Pattern.compile("\\b(debug|debugger|debugging|error|exception|stack.?trace|traceback)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "an expert software engineer and debugger"),

            // Code writing / implementation
            new RolePattern(
                    Pattern.compile("\\b(code|implement|function|algorithm|script|class|method|refactor|snippet)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "a skilled software developer"),

            // Data analysis
            new RolePattern(
                    Pattern.compile("\\b(data|analyz(?:e[sd]?|ing|is)|statistics|statistical|chart|visuali[sz]e|csv|dataset|regression|correlation)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "an expert data analyst"),

            // Translation
            new RolePattern(
                    Pattern.compile("\\b(translat[es]|(?:in|to)\\s+(?:French|Spanish|German|Arabic|Japanese|Chinese)|convert.*language|language.*convert)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "a professional translator"),

            // Creative writing
            new RolePattern(
                    Pattern.compile("\\b(poem|story|creative|narrative|fiction|novel|screenplay|lyrics|haiku|sonnet)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "a creative writer"),

            // Medical / health information
            new RolePattern(
                    Pattern.compile("\\b(medical|symptom[s]?|diagnosis|diagnose|treatment|disease|medication|drug|prescription|clinical|health)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "a knowledgeable medical information assistant"),

            // Legal information
            new RolePattern(
                    Pattern.compile("\\b(legal|law|contract|regulation|compliance|statute|liability|court|attorney|lawsuit|rights)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "a legal information assistant"),

            // Security / cybersecurity
            new RolePattern(
                    Pattern.compile("\\b(security|vulnerabilit(?:y|ies)|penetration|exploit|hack|malware|phishing|CVE|OWASP|firewall|authentication)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "a cybersecurity expert"),

            // Mathematics
            new RolePattern(
                    Pattern.compile("\\b(math|equation|calculus|derivative|integral|proof|geometry|algebra|trigonometry|matrix|vector|theorem)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "an expert mathematician"),

            // Teaching / explanation
            new RolePattern(
                    Pattern.compile("\\b(explain|teach|tutorial|how does|what is|how do|step.?by.?step|beginner|introduction to)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "a clear and patient teacher")
    );

    public RoleDetectionService(
            @Value("${pipeline.role.detection.enabled:true}") boolean detectionEnabled) {
        this.detectionEnabled = detectionEnabled;
    }

    /**
     * Infers a role from the supplied user input.
     *
     * @param userInput the raw user message; may be {@code null}
     * @return a role string if a pattern matched, or {@code null} when detection
     *         is disabled or no pattern matched
     */
    public String detectRole(String userInput) {
        if (!detectionEnabled || userInput == null || userInput.isBlank()) {
            return null;
        }
        for (RolePattern rp : PATTERNS) {
            if (rp.pattern().matcher(userInput).find()) {
                return rp.role();
            }
        }
        return null;
    }
}
