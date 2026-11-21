package com.jawaher.GPT.DTO;

/**
 * Incoming request payload for all chat endpoints.
 * <p>
 * {@code message} is the only required field; all other fields are optional
 * and control the behaviour of the prompt transformation pipeline.
 */
public class ChatRequest {

    /** Required. The raw user message to send to the LLM(s). */
    private String message;

    /**
     * Optional. Persona / role override passed to T2 (Role Conditioning).
     * Example: {@code "senior Java engineer"}.
     */
    private String role;

    /**
     * Optional. Output-format hint passed to T3 (Output Format Specification).
     * Recognised values: {@code bullet_points}, {@code json},
     * {@code numbered_list}, {@code prose}, {@code sections}.
     */
    private String outputFormat;

    /**
     * Optional. Background context passed to T4 (Context Delimitation).
     * The pipeline wraps this in clear delimiters before including it in the
     * prompt.
     */
    private String context;

    /**
     * Optional. Per-request token budget hint passed to T7 (Token Budgeting).
     * When set, it is used as an additional upper bound on the effective token
     * budget (alongside the per-provider defaults).
     */
    private Integer maxTokens;

    /**
     * Optional. Task category (e.g. {@code "Comparative reasoning"} or
     * {@code "COMPARATIVE"}) used by T1 (Task Classification). When absent the
     * task type is inferred from the message.
     */
    private String taskType;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }
}
