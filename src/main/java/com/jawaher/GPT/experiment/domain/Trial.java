package com.jawaher.GPT.experiment.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One experimental trial: a single prompt submitted by a participant, executed
 * under both conditions (raw and transformed) on every configured provider.
 * <p>
 * Stores the raw request, the full Prompt IR and the compiled (transformed)
 * prompts, plus the participant's overall "best model" judgement.
 */
@Entity
@Table(name = "trials", indexes = @Index(name = "idx_trials_participant", columnList = "participant_id"))
public class Trial {

    public static final int LONG_TEXT = 100_000;

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id")
    private Participant participant;

    @Column(nullable = false)
    private Instant createdAt;

    /** Task category chosen by the participant (Factual QA, Comparative reasoning, ...). */
    @Column(length = 60)
    private String taskCategory;

    /** The participant's prompt exactly as typed (the RAW condition input). */
    @Column(nullable = false, length = LONG_TEXT)
    private String rawPrompt;

    /** Prompt IR after the pipeline ran, serialised as JSON. */
    @Column(length = LONG_TEXT)
    private String promptIrJson;

    /** All provider-specific compiled prompts (system + user text), serialised as JSON. */
    @Column(length = LONG_TEXT)
    private String transformedPromptsJson;

    /** Comma-separated provider ids in the (shuffled) order shown as Model 1, 2, 3. */
    @Column(length = 120)
    private String providerDisplayOrder;

    /** Provider id judged best overall, or "none". Null until evaluated. */
    @Column(length = 30)
    private String bestProvider;

    @Column(length = 5_000)
    private String bestProviderReason;

    private Instant evaluatedAt;

    @OneToMany(mappedBy = "trial", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("provider ASC, condition ASC")
    private List<LlmResponse> responses = new ArrayList<>();

    @OneToMany(mappedBy = "trial", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("provider ASC")
    private List<Vote> votes = new ArrayList<>();

    protected Trial() {
    }

    public Trial(Participant participant, String taskCategory, String rawPrompt) {
        this.id = UUID.randomUUID();
        this.participant = participant;
        this.createdAt = Instant.now();
        this.taskCategory = taskCategory;
        this.rawPrompt = rawPrompt;
    }

    public void addResponse(LlmResponse response) {
        responses.add(response);
    }

    public void addVote(Vote vote) {
        votes.add(vote);
    }

    public boolean isEvaluated() {
        return evaluatedAt != null;
    }

    public void markEvaluated(String bestProvider, String bestProviderReason) {
        this.bestProvider = bestProvider;
        this.bestProviderReason = bestProviderReason;
        this.evaluatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Participant getParticipant() { return participant; }
    public Instant getCreatedAt() { return createdAt; }
    public String getTaskCategory() { return taskCategory; }
    public String getRawPrompt() { return rawPrompt; }
    public String getPromptIrJson() { return promptIrJson; }
    public void setPromptIrJson(String promptIrJson) { this.promptIrJson = promptIrJson; }
    public String getTransformedPromptsJson() { return transformedPromptsJson; }
    public void setTransformedPromptsJson(String json) { this.transformedPromptsJson = json; }
    public String getProviderDisplayOrder() { return providerDisplayOrder; }
    public void setProviderDisplayOrder(String order) { this.providerDisplayOrder = order; }
    public String getBestProvider() { return bestProvider; }
    public String getBestProviderReason() { return bestProviderReason; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public List<LlmResponse> getResponses() { return responses; }
    public List<Vote> getVotes() { return votes; }
}
