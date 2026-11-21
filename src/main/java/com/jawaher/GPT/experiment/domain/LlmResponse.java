package com.jawaher.GPT.experiment.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One LLM call within a trial: a (provider, condition) cell. Stores the exact
 * prompt that was sent, the response, the model id, latency and failure info,
 * and the blinded label ("A"/"B") under which it was shown.
 */
@Entity
@Table(name = "llm_responses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trial_id", "provider", "prompt_condition"}))
public class LlmResponse {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trial_id")
    private Trial trial;

    /** Provider id: openai, claude or gemini. */
    @Column(nullable = false, length = 30)
    private String provider;

    @Column(length = 80)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "prompt_condition", nullable = false, length = 20)
    private PromptCondition condition;

    /** System prompt sent to the provider (null/empty for the RAW condition). */
    @Column(length = Trial.LONG_TEXT)
    private String systemPrompt;

    /** User message sent to the provider. */
    @Column(nullable = false, length = Trial.LONG_TEXT)
    private String userPrompt;

    @Column(length = Trial.LONG_TEXT)
    private String responseText;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 2_000)
    private String errorMessage;

    private long latencyMs;

    /** Blinded label shown to the participant: "A" or "B". */
    @Column(nullable = false, length = 1)
    private String displayLabel;

    @Column(nullable = false)
    private Instant createdAt;

    protected LlmResponse() {
    }

    public LlmResponse(Trial trial, String provider, String model, PromptCondition condition,
                       String systemPrompt, String userPrompt, String displayLabel) {
        this.id = UUID.randomUUID();
        this.trial = trial;
        this.provider = provider;
        this.model = model;
        this.condition = condition;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
        this.displayLabel = displayLabel;
        this.createdAt = Instant.now();
    }

    public void recordSuccess(String text, long latencyMs) {
        this.success = true;
        this.responseText = text;
        this.latencyMs = latencyMs;
    }

    public void recordFailure(String error, long latencyMs) {
        this.success = false;
        this.errorMessage = error == null ? null : error.substring(0, Math.min(error.length(), 2_000));
        this.latencyMs = latencyMs;
    }

    public UUID getId() { return id; }
    public Trial getTrial() { return trial; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public PromptCondition getCondition() { return condition; }
    public String getSystemPrompt() { return systemPrompt; }
    public String getUserPrompt() { return userPrompt; }
    public String getResponseText() { return responseText; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public long getLatencyMs() { return latencyMs; }
    public String getDisplayLabel() { return displayLabel; }
    public Instant getCreatedAt() { return createdAt; }
}
