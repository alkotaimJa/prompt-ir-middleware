package com.jawaher.GPT.experiment.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * An anonymous study participant. No personally identifying data is required;
 * {@code alias} is optional and free text.
 */
@Entity
@Table(name = "participants")
public class Participant {

    @Id
    private UUID id;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 80)
    private String alias;

    /** e.g. student, software developer, researcher, other. */
    @Column(length = 60)
    private String background;

    /** Self-rated familiarity with LLM tools, 1 (none) – 5 (expert). */
    private Integer llmFamiliarity;

    @Column(nullable = false)
    private boolean consentGiven;

    protected Participant() {
    }

    public Participant(String alias, String background, Integer llmFamiliarity, boolean consentGiven) {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.alias = alias;
        this.background = background;
        this.llmFamiliarity = llmFamiliarity;
        this.consentGiven = consentGiven;
    }

    public UUID getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public String getAlias() { return alias; }
    public String getBackground() { return background; }
    public Integer getLlmFamiliarity() { return llmFamiliarity; }
    public boolean isConsentGiven() { return consentGiven; }
}
