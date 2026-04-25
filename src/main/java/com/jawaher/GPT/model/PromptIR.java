package com.jawaher.GPT.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Prompt Intermediate Representation (Prompt IR).
 * <p>
 * Carries the structured, provider-neutral representation of a prompt as it
 * travels through the transformation pipeline. Each transformer reads and/or
 * writes one or more fields; the final state is consumed by the service layer
 * to drive the actual LLM calls.
 */
public class PromptIR {

    /** Raw user message, preserved verbatim throughout the pipeline. */
    private String userInput;

    /** Scaffolded task instruction produced by T1 and optionally extended by T3. */
    private String taskInstruction;

    /** Resolved persona / role string produced by T2. */
    private String role;

    /** Output-format identifier copied from the original request (e.g. "json"). */
    private String outputFormat;

    /** Delimited context block produced by T4; empty string when no context was supplied. */
    private String contextBlock;

    /**
     * Effective token budget (in tokens) for this request, set by T7.
     * Represents the most restrictive provider budget that applies to the
     * current set of compiled prompts.
     */
    private int tokenBudget;

    /**
     * Provider-specific compiled prompts produced by T6.
     * Key: provider name constant (e.g. {@code "openai"}, {@code "claude"}, {@code "gemini"}).
     */
    private final Map<String, CompiledPrompt> compiledPrompts = new HashMap<>();

    /**
     * Arbitrary provider-specific metadata that individual transformers or
     * compilers may attach for downstream use.
     */
    private final Map<String, Object> providerHints = new HashMap<>();

    public PromptIR(String userInput) {
        this.userInput = userInput;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public String getTaskInstruction() {
        return taskInstruction;
    }

    public void setTaskInstruction(String taskInstruction) {
        this.taskInstruction = taskInstruction;
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

    public String getContextBlock() {
        return contextBlock;
    }

    public void setContextBlock(String contextBlock) {
        this.contextBlock = contextBlock;
    }

    public int getTokenBudget() {
        return tokenBudget;
    }

    public void setTokenBudget(int tokenBudget) {
        this.tokenBudget = tokenBudget;
    }

    public Map<String, CompiledPrompt> getCompiledPrompts() {
        return compiledPrompts;
    }

    public Map<String, Object> getProviderHints() {
        return providerHints;
    }
}
