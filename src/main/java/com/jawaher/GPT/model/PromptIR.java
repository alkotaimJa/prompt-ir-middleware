package com.jawaher.GPT.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prompt Intermediate Representation (Prompt IR), version 2.
 * <p>
 * Provider-neutral, structured representation of a prompt. Each transformer
 * reads and/or writes specific fields; provider compilers render the final
 * state into provider-specific system and user messages. The user's original
 * words ({@link #userInput}) are never modified by any stage.
 */
public class PromptIR {

    /** Raw user message, preserved verbatim throughout the pipeline. */
    private String userInput;

    /** Task type (T1), from the participant's category or inferred from the input. */
    private TaskType taskType;

    /** Intended audience extracted from the input (T2), e.g. "a beginner with no prior background". */
    private String audience;

    /** Explicit constraints extracted from the input (T2), e.g. "Provide exactly 5 bullet points." */
    private final List<String> constraints = new ArrayList<>();

    /** True when the user stated an explicit length/count constraint (T2); disables the length guard. */
    private boolean explicitLengthConstraint;

    /** Task-specific instruction (T3). */
    private String taskInstruction;

    /** Task-specific success criteria (T3). */
    private final List<String> successCriteria = new ArrayList<>();

    /** Role inferred from content and task type (T4); {@code null} when no role is justified. */
    private String detectedRole;

    /** Final persona (T5); empty when no role is used. */
    private String role;

    /** Output-format identifier (T6), e.g. "table", "bullet_points", "code". */
    private String outputFormat;

    /** Natural-language directive for the output format (T6). */
    private String formatDirective;

    /** Length guidance (T7); empty when the user gave an explicit length constraint. */
    private String lengthGuidance;

    /** Supporting context supplied by the user (T8), without delimiters. */
    private String contextBlock;

    /** Additional response guidelines (T9): assumptions/uncertainty, planning, self-check. */
    private final List<String> responseGuidelines = new ArrayList<>();

    /** Effective input token budget (S3). */
    private int tokenBudget;

    /** Ids of the layers that were applied, in order (for reproducibility and ablation). */
    private final List<String> appliedLayers = new ArrayList<>();

    /** Provider-specific compiled prompts (S2). Key: provider id. */
    private final Map<String, CompiledPrompt> compiledPrompts = new HashMap<>();

    /** Arbitrary provider-specific metadata for downstream use. */
    private final Map<String, Object> providerHints = new HashMap<>();

    public PromptIR(String userInput) {
        this.userInput = userInput;
    }

    public String getUserInput() { return userInput; }
    public void setUserInput(String userInput) { this.userInput = userInput; }
    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public List<String> getConstraints() { return constraints; }
    public boolean isExplicitLengthConstraint() { return explicitLengthConstraint; }
    public void setExplicitLengthConstraint(boolean v) { this.explicitLengthConstraint = v; }
    public String getTaskInstruction() { return taskInstruction; }
    public void setTaskInstruction(String taskInstruction) { this.taskInstruction = taskInstruction; }
    public List<String> getSuccessCriteria() { return successCriteria; }
    public String getDetectedRole() { return detectedRole; }
    public void setDetectedRole(String detectedRole) { this.detectedRole = detectedRole; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
    public String getFormatDirective() { return formatDirective; }
    public void setFormatDirective(String formatDirective) { this.formatDirective = formatDirective; }
    public String getLengthGuidance() { return lengthGuidance; }
    public void setLengthGuidance(String lengthGuidance) { this.lengthGuidance = lengthGuidance; }
    public String getContextBlock() { return contextBlock; }
    public void setContextBlock(String contextBlock) { this.contextBlock = contextBlock; }
    public List<String> getResponseGuidelines() { return responseGuidelines; }
    public int getTokenBudget() { return tokenBudget; }
    public void setTokenBudget(int tokenBudget) { this.tokenBudget = tokenBudget; }
    public List<String> getAppliedLayers() { return appliedLayers; }
    public Map<String, CompiledPrompt> getCompiledPrompts() { return compiledPrompts; }
    public Map<String, Object> getProviderHints() { return providerHints; }
}
