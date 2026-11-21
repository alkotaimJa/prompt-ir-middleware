package com.jawaher.GPT.services;

import com.jawaher.GPT.model.TaskType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Infers a domain-expert persona for a request (used by T4).
 * <p>
 * Version 2 rules are deliberately conservative. Version 1 matched single,
 * ambiguous words and produced wrong personas (e.g. "Moore's law" → legal
 * assistant, "margin of error" → debugger, "class struggle" → software
 * developer) and sent almost every "what is / explain" question to a generic
 * teacher. Version 2:
 * <ol>
 *   <li>uses the task type first (coding tasks → software engineer),</li>
 *   <li>matches only unambiguous domain terms or multi-word phrases,</li>
 *   <li>uses a teacher persona only when the audience is a beginner or child,</li>
 *   <li>returns {@code null} otherwise, so no generic persona is added.</li>
 * </ol>
 */
@Component
public class RoleDetectionService {

    private record RolePattern(Pattern pattern, String role) {
    }

    private static final int FLAGS = Pattern.CASE_INSENSITIVE;

    private static final Pattern DEBUGGING = Pattern.compile(
            "\\b(debug|debugging|bug|stack ?trace|traceback|exception|compile error|syntax error|error message|"
                    + "not working|doesn'?t work|crash(es|ed)?)\\b", FLAGS);

    private static final List<RolePattern> DOMAIN_PATTERNS = List.of(
            new RolePattern(Pattern.compile(
                    "\\b(cyber ?security|vulnerabilit(y|ies)|penetration test(ing)?|malware|ransomware|phishing|owasp|"
                            + "cve-\\d+|firewall|encryption|xss|sql injection)\\b", FLAGS),
                    "a cybersecurity specialist"),
            new RolePattern(Pattern.compile(
                    "\\b(dataset|data analysis|data analytics|statistics|statistical|regression|correlation|csv|pandas|"
                            + "data visuali[sz]ation|hypothesis test(ing)?)\\b", FLAGS),
                    "an experienced data analyst"),
            new RolePattern(Pattern.compile(
                    "\\b(math|mathematics|calculus|derivatives?|integrals?|theorem|equations?|algebra|geometry|"
                            + "trigonometry|probability)\\b", FLAGS),
                    "a mathematics expert"),
            new RolePattern(Pattern.compile(
                    "\\b(symptoms?|diagnos(is|e|ed)|medications?|diseases?|clinical|prescriptions?|dosage)\\b", FLAGS),
                    "a medical information specialist"),
            new RolePattern(Pattern.compile(
                    "\\b(legal|lawsuit|attorney|lawyer|statute|legislation|copyright law|contract law|tort|court case)\\b", FLAGS),
                    "a legal information specialist"),
            new RolePattern(Pattern.compile("\\btranslat(e|es|ed|ion|ing)\\b", FLAGS),
                    "a professional translator"),
            new RolePattern(Pattern.compile(
                    "\\b(poem|short story|fiction|screenplay|song lyrics|haiku|sonnet)\\b", FLAGS),
                    "a creative writer"),
            new RolePattern(Pattern.compile(
                    "\\b(apis?|rest(ful)?|graphql|soap|microservices?|databases?|sql|nosql|postgresql|mysql|mongodb|"
                            + "software architecture|design patterns?|cloud computing|kubernetes|docker|http|devops|"
                            + "agile|scrum|waterfall|software engineering|unit test(s|ing)?)\\b", FLAGS),
                    "an experienced software engineer")
    );

    private final boolean detectionEnabled;

    public RoleDetectionService(@Value("${pipeline.role.detection.enabled:true}") boolean detectionEnabled) {
        this.detectionEnabled = detectionEnabled;
    }

    /**
     * Infers a persona.
     *
     * @param userInput raw user message (may be {@code null})
     * @param taskType  task type from T1 (may be {@code null})
     * @param audience  audience from T2 (may be {@code null})
     * @return a persona, or {@code null} when none is justified or detection is disabled
     */
    public String detectRole(String userInput, TaskType taskType, String audience) {
        if (!detectionEnabled || userInput == null || userInput.isBlank()) {
            return null;
        }
        if (taskType == TaskType.CODING) {
            return DEBUGGING.matcher(userInput).find()
                    ? "an expert software engineer experienced in debugging"
                    : "an experienced software engineer";
        }
        for (RolePattern rp : DOMAIN_PATTERNS) {
            if (rp.pattern().matcher(userInput).find()) {
                return rp.role();
            }
        }
        if (audience != null && (audience.startsWith("a beginner") || audience.startsWith("a child"))) {
            return "a patient teacher";
        }
        return null;
    }

    /** Convenience overload without task type or audience. */
    public String detectRole(String userInput) {
        return detectRole(userInput, null, null);
    }
}
