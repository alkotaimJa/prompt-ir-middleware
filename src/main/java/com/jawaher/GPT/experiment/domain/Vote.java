package com.jawaher.GPT.experiment.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A participant's blinded pairwise judgement for one provider within a trial
 * (raw vs transformed), together with their free-text reasoning.
 */
@Entity
@Table(name = "votes", uniqueConstraints = @UniqueConstraint(columnNames = {"trial_id", "provider"}))
public class Vote {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trial_id")
    private Trial trial;

    @Column(nullable = false, length = 30)
    private String provider;

    /** What the participant clicked (blinded A/B). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoteChoice choice;

    /** Un-blinded result: which condition was preferred. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Preference preference;

    /** The participant's explanation of why they chose this. */
    @Column(length = 5_000)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    protected Vote() {
    }

    public Vote(Trial trial, String provider, VoteChoice choice, Preference preference, String reason) {
        this.id = UUID.randomUUID();
        this.trial = trial;
        this.provider = provider;
        this.choice = choice;
        this.preference = preference;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Trial getTrial() { return trial; }
    public String getProvider() { return provider; }
    public VoteChoice getChoice() { return choice; }
    public Preference getPreference() { return preference; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
