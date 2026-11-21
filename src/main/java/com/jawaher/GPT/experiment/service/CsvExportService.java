package com.jawaher.GPT.experiment.service;

import com.jawaher.GPT.experiment.domain.LlmResponse;
import com.jawaher.GPT.experiment.domain.Participant;
import com.jawaher.GPT.experiment.domain.PromptCondition;
import com.jawaher.GPT.experiment.domain.Trial;
import com.jawaher.GPT.experiment.domain.Vote;
import com.jawaher.GPT.experiment.repository.LlmResponseRepository;
import com.jawaher.GPT.experiment.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Exports the raw study data as CSV (RFC 4180) for offline statistical analysis
 * (e.g. Python/pandas, R, SPSS).
 */
@Service
public class CsvExportService {

    private final VoteRepository votes;
    private final LlmResponseRepository responses;

    public CsvExportService(VoteRepository votes, LlmResponseRepository responses) {
        this.votes = votes;
        this.responses = responses;
    }

    /** One row per vote (participant × trial × provider), with both prompts and both responses. */
    @Transactional(readOnly = true)
    public String votesCsv() {
        StringBuilder sb = new StringBuilder();
        row(sb, "vote_id", "voted_at", "trial_id", "trial_created_at", "participant_id", "participant_background",
                "participant_llm_familiarity", "task_category", "provider", "model", "raw_prompt",
                "transformed_system_prompt", "transformed_user_prompt", "raw_response", "transformed_response",
                "raw_shown_as", "choice", "preference", "reason", "raw_latency_ms", "transformed_latency_ms",
                "best_model_overall", "best_model_reason", "raw_response_chars", "transformed_response_chars",
                "pipeline_version", "context_options", "context_generator", "selected_context", "prompt_ir_json");
        for (Vote v : votes.findAllWithTrial()) {
            Trial t = v.getTrial();
            Participant p = t.getParticipant();
            LlmResponse raw = find(t, v.getProvider(), PromptCondition.RAW);
            LlmResponse tr = find(t, v.getProvider(), PromptCondition.TRANSFORMED);
            row(sb, v.getId(), v.getCreatedAt(), t.getId(), t.getCreatedAt(), p.getId(), p.getBackground(),
                    p.getLlmFamiliarity(), t.getTaskCategory(), v.getProvider(), raw == null ? null : raw.getModel(),
                    t.getRawPrompt(),
                    tr == null ? null : tr.getSystemPrompt(), tr == null ? null : tr.getUserPrompt(),
                    raw == null ? null : raw.getResponseText(), tr == null ? null : tr.getResponseText(),
                    raw == null ? null : raw.getDisplayLabel(), v.getChoice(), v.getPreference(), v.getReason(),
                    raw == null ? null : raw.getLatencyMs(), tr == null ? null : tr.getLatencyMs(),
                    t.getBestProvider(), t.getBestProviderReason(),
                    raw == null || raw.getResponseText() == null ? null : raw.getResponseText().length(),
                    tr == null || tr.getResponseText() == null ? null : tr.getResponseText().length(),
                    t.getPipelineVersion(), t.getContextOptionsJson(), t.getContextGenerator(), t.getSelectedContext(),
                    t.getPromptIrJson());
        }
        return sb.toString();
    }

    /** One row per LLM call, including failed calls and trials that were never evaluated. */
    @Transactional(readOnly = true)
    public String responsesCsv() {
        StringBuilder sb = new StringBuilder();
        row(sb, "response_id", "created_at", "trial_id", "task_category", "provider", "model", "condition",
                "shown_as", "system_prompt", "user_prompt", "response_text", "response_chars", "success", "error",
                "latency_ms", "pipeline_version", "selected_context");
        for (LlmResponse r : responses.findAllWithTrial()) {
            Trial t = r.getTrial();
            row(sb, r.getId(), r.getCreatedAt(), t.getId(), t.getTaskCategory(), r.getProvider(), r.getModel(),
                    r.getCondition(), r.getDisplayLabel(), r.getSystemPrompt(), r.getUserPrompt(),
                    r.getResponseText(), r.getResponseText() == null ? null : r.getResponseText().length(),
                    r.isSuccess(), r.getErrorMessage(), r.getLatencyMs(), t.getPipelineVersion(), t.getSelectedContext());
        }
        return sb.toString();
    }

    private static LlmResponse find(Trial t, String provider, PromptCondition c) {
        Optional<LlmResponse> r = t.getResponses().stream()
                .filter(x -> x.getProvider().equals(provider) && x.getCondition() == c)
                .findFirst();
        return r.orElse(null);
    }

    private static void row(StringBuilder sb, Object... cells) {
        StringJoiner j = new StringJoiner(",");
        for (Object c : cells) {
            j.add(escape(c == null ? "" : Objects.toString(c)));
        }
        sb.append(j).append("\r\n");
    }

    static String escape(String s) {
        // Neutralise spreadsheet formula injection ('-' is left alone because
        // Markdown list responses legitimately start with it), then quote per RFC 4180.
        if (!s.isEmpty() && "=+@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
