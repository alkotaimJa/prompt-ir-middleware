package com.jawaher.GPT.experiment.web;

import com.jawaher.GPT.experiment.domain.VoteChoice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Request/response payloads of the experiment API. */
public final class Dtos {

    private Dtos() {
    }

    // ── Participants ────────────────────────────────────────────────────────

    public record RegisterParticipantRequest(
            @Size(max = 80) String alias,
            @Size(max = 60) String background,
            @Min(1) @Max(5) Integer llmFamiliarity,
            boolean consent,
            @Size(max = 100) String accessCode) {
    }

    public record ParticipantView(UUID participantId, long trialsStarted, long trialsEvaluated, int maxTrials) {
    }

    // ── Trials ──────────────────────────────────────────────────────────────

    public record ContextOptionsRequest(
            @NotNull UUID participantId,
            @Size(max = 60) String taskCategory,
            @NotBlank String prompt) {
    }

    /** Clarifying context options; the token identifies them when the trial is started. */
    public record ContextOptionsView(String contextToken, List<String> options) {
    }

    public record StartTrialRequest(
            @NotNull UUID participantId,
            @Size(max = 60) String taskCategory,
            @NotBlank String prompt,
            /** Token from /api/trials/context-options; null when the step was skipped or disabled. */
            @Size(max = 64) String contextToken,
            /** Index of the selected option; null or negative = "none of these". */
            Integer selectedContextIndex) {
    }

    /** One blinded response as shown to the participant. */
    public record BlindResponse(String label, boolean ok, String text) {
    }

    /** One model card: provider identity hidden behind "Model N". */
    public record ModelPair(int slot, String modelLabel, boolean votable, BlindResponse responseA, BlindResponse responseB) {
    }

    public record TrialView(UUID trialId, String prompt, String selectedContext, List<ModelPair> models,
                            boolean anyVotable, long trialsRemaining) {
    }

    public record VoteRequest(@Min(1) int slot, @NotNull VoteChoice choice, @Size(max = 5000) String reason) {
    }

    public record EvaluationRequest(
            @NotNull UUID participantId,
            @NotNull @Valid List<VoteRequest> votes,
            /** 1..N for "Model N", 0 for "none of them". */
            @Min(0) int bestSlot,
            @Size(max = 5000) String bestReason) {
    }

    public record EvaluationResult(UUID trialId, int votesRecorded, long trialsEvaluated, long trialsRemaining) {
    }

    public record StudyInfo(boolean accessCodeRequired, List<String> taskCategories, int maxPromptChars,
                            int minReasonChars, boolean contextStepEnabled, boolean mockMode) {
    }
}
