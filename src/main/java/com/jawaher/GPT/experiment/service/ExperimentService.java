package com.jawaher.GPT.experiment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.experiment.ExperimentProperties;
import com.jawaher.GPT.experiment.domain.*;
import com.jawaher.GPT.experiment.llm.LlmGateway;
import com.jawaher.GPT.experiment.repository.ParticipantRepository;
import com.jawaher.GPT.experiment.repository.TrialRepository;
import com.jawaher.GPT.experiment.web.ApiException;
import com.jawaher.GPT.experiment.web.Dtos.*;
import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Orchestrates the raw-vs-transformed experiment.
 * <p>
 * For every trial the participant's prompt is executed on each provider twice
 * — once verbatim (RAW) and once after the Prompt IR pipeline (TRANSFORMED) —
 * concurrently and under identical runtime conditions. The two responses are
 * shown as "Response A" / "Response B" in a per-provider random order, and the
 * providers themselves are shown as "Model 1..N" in a random order, so the
 * participant is blind to both the condition and the provider.
 */
@Service
public class ExperimentService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentService.class);

    /**
     * Task categories offered to participants. Summarisation is deliberately
     * excluded: its output is dominated by the source text, leaving little room
     * for prompt structure to change the response (supervisor decision).
     */
    public static final List<String> TASK_CATEGORIES = List.of(
            "Factual QA", "Analytical explanation",
            "Instruction following", "Comparative reasoning", "Coding", "Other");

    private final PromptPipeline pipeline;
    private final LlmGateway llm;
    private final ParticipantRepository participants;
    private final TrialRepository trials;
    private final ExperimentProperties props;
    private final Executor executor;
    private final ObjectMapper json;
    private final RateLimiter rateLimiter;
    private final Random random = new SecureRandom();

    public ExperimentService(PromptPipeline pipeline,
                             LlmGateway llm,
                             ParticipantRepository participants,
                             TrialRepository trials,
                             ExperimentProperties props,
                             @Qualifier("llmTaskExecutor") Executor executor,
                             ObjectMapper json,
                             RateLimiter rateLimiter) {
        this.pipeline = pipeline;
        this.llm = llm;
        this.participants = participants;
        this.trials = trials;
        this.props = props;
        this.executor = executor;
        this.json = json;
        this.rateLimiter = rateLimiter;
    }

    // ── Study info & participants ───────────────────────────────────────────

    public StudyInfo studyInfo() {
        return new StudyInfo(!isBlank(props.getAccessCode()), TASK_CATEGORIES,
                props.getMaxPromptChars(), props.getMinReasonChars(), props.isMockLlm());
    }

    @Transactional
    public ParticipantView register(RegisterParticipantRequest req) {
        if (!req.consent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Consent is required to take part in the study.");
        }
        if (!isBlank(props.getAccessCode())
                && !props.getAccessCode().trim().equals(req.accessCode() == null ? "" : req.accessCode().trim())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid access code.");
        }
        Participant p = participants.save(new Participant(
                trimToNull(req.alias()), trimToNull(req.background()), req.llmFamiliarity(), true));
        return new ParticipantView(p.getId(), 0, 0, props.getMaxTrialsPerParticipant());
    }

    @Transactional(readOnly = true)
    public ParticipantView participant(UUID id) {
        Participant p = participants.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown participant."));
        return new ParticipantView(p.getId(),
                trials.countByParticipantId(id),
                trials.countByParticipantIdAndEvaluatedAtIsNotNull(id),
                props.getMaxTrialsPerParticipant());
    }

    // ── Trials ──────────────────────────────────────────────────────────────

    public TrialView startTrial(StartTrialRequest req, String clientIp) {
        String prompt = req.prompt() == null ? "" : req.prompt().strip();
        if (prompt.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please enter a prompt.");
        }
        if (prompt.length() > props.getMaxPromptChars()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Prompt is too long (max " + props.getMaxPromptChars() + " characters).");
        }
        Participant participant = participants.findById(req.participantId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown participant. Please register again."));
        long started = trials.countByParticipantId(participant.getId());
        if (started >= props.getMaxTrialsPerParticipant()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "You have reached the maximum number of prompts for this study. Thank you!");
        }
        if (!rateLimiter.tryAcquire(clientIp)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.");
        }

        String category = TASK_CATEGORIES.contains(req.taskCategory()) ? req.taskCategory() : "Other";
        Trial trial = new Trial(participant, category, prompt);

        // 1) Transformation: raw prompt → Prompt IR → provider-specific compiled prompts.
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMessage(prompt);
        PromptIR ir = pipeline.process(chatRequest);
        trial.setPromptIrJson(toJson(irSnapshot(ir)));
        trial.setTransformedPromptsJson(toJson(ir.getCompiledPrompts()));

        // 2) Blinding: shuffle provider order and A/B position per provider.
        List<String> providers = new ArrayList<>(props.getProviders());
        Collections.shuffle(providers, random);
        trial.setProviderDisplayOrder(String.join(",", providers));

        Map<LlmResponse, CompletableFuture<Void>> calls = new LinkedHashMap<>();
        for (String provider : providers) {
            CompiledPrompt compiled = ir.getCompiledPrompts().get(provider);
            if (compiled == null) {
                throw new IllegalStateException("No compiler registered for provider " + provider);
            }
            boolean rawIsA = random.nextBoolean();
            LlmResponse raw = new LlmResponse(trial, provider, llm.modelFor(provider), PromptCondition.RAW,
                    null, prompt, rawIsA ? "A" : "B");
            LlmResponse transformed = new LlmResponse(trial, provider, llm.modelFor(provider), PromptCondition.TRANSFORMED,
                    compiled.systemText(), compiled.userText(), rawIsA ? "B" : "A");
            trial.addResponse(raw);
            trial.addResponse(transformed);
            calls.put(raw, execute(raw));
            calls.put(transformed, execute(transformed));
        }

        // 3) Execute all 2×N calls concurrently and wait for every one to finish.
        CompletableFuture.allOf(calls.values().toArray(CompletableFuture[]::new)).join();

        trials.save(trial);
        return toView(trial, providers, started + 1);
    }

    private CompletableFuture<Void> execute(LlmResponse cell) {
        long start = System.nanoTime();
        return CompletableFuture
                .supplyAsync(() -> llm.complete(cell.getProvider(), cell.getSystemPrompt(), cell.getUserPrompt()), executor)
                .orTimeout(props.getCallTimeoutSeconds(), TimeUnit.SECONDS)
                .handle((text, ex) -> {
                    long ms = (System.nanoTime() - start) / 1_000_000;
                    if (ex == null) {
                        cell.recordSuccess(text, ms);
                    } else {
                        Throwable root = rootCause(ex);
                        String msg = root instanceof TimeoutException
                                ? "Timed out after " + props.getCallTimeoutSeconds() + "s"
                                : root.getClass().getSimpleName() + ": " + root.getMessage();
                        log.error("{} {} call failed: {}", cell.getProvider(), cell.getCondition(), msg);
                        cell.recordFailure(msg, ms);
                    }
                    return null;
                });
    }

    private TrialView toView(Trial trial, List<String> providerOrder, long startedSoFar) {
        List<ModelPair> pairs = new ArrayList<>();
        for (int i = 0; i < providerOrder.size(); i++) {
            String provider = providerOrder.get(i);
            LlmResponse a = cell(trial, provider, "A");
            LlmResponse b = cell(trial, provider, "B");
            boolean votable = a.isSuccess() && b.isSuccess();
            pairs.add(new ModelPair(i + 1, "Model " + (i + 1), votable, blind(a), blind(b)));
        }
        long remaining = Math.max(0, props.getMaxTrialsPerParticipant() - startedSoFar);
        return new TrialView(trial.getId(), trial.getRawPrompt(), pairs, remaining);
    }

    private static BlindResponse blind(LlmResponse r) {
        return r.isSuccess()
                ? new BlindResponse(r.getDisplayLabel(), true, r.getResponseText())
                : new BlindResponse(r.getDisplayLabel(), false, "This model could not answer right now.");
    }

    private static LlmResponse cell(Trial trial, String provider, String label) {
        return trial.getResponses().stream()
                .filter(r -> r.getProvider().equals(provider) && r.getDisplayLabel().equals(label))
                .findFirst()
                .orElseThrow();
    }

    // ── Evaluation (votes + reasons + best model) ───────────────────────────

    @Transactional
    public EvaluationResult evaluate(UUID trialId, EvaluationRequest req) {
        Trial trial = trials.findById(trialId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown trial."));
        if (!trial.getParticipant().getId().equals(req.participantId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This trial belongs to another participant.");
        }
        if (trial.isEvaluated()) {
            throw new ApiException(HttpStatus.CONFLICT, "This trial has already been evaluated.");
        }

        List<String> order = Arrays.asList(trial.getProviderDisplayOrder().split(","));
        Map<Integer, VoteRequest> bySlot = new HashMap<>();
        for (VoteRequest v : req.votes()) {
            if (v.slot() < 1 || v.slot() > order.size()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid model number " + v.slot() + ".");
            }
            if (bySlot.put(v.slot(), v) != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Duplicate vote for Model " + v.slot() + ".");
            }
        }

        int recorded = 0;
        for (int i = 0; i < order.size(); i++) {
            int slot = i + 1;
            String provider = order.get(i);
            LlmResponse a = cell(trial, provider, "A");
            LlmResponse b = cell(trial, provider, "B");
            if (!(a.isSuccess() && b.isSuccess())) {
                continue; // nothing to compare for a failed provider
            }
            VoteRequest v = bySlot.get(slot);
            if (v == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Please vote for Model " + slot + ".");
            }
            String reason = v.reason() == null ? "" : v.reason().strip();
            if (reason.length() < props.getMinReasonChars()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Please briefly explain your choice for Model " + slot + ".");
            }
            boolean rawIsA = a.getCondition() == PromptCondition.RAW;
            trial.addVote(new Vote(trial, provider, v.choice(), Preference.resolve(v.choice(), rawIsA), reason));
            recorded++;
        }

        String best;
        if (req.bestSlot() == 0) {
            best = "none";
        } else if (req.bestSlot() <= order.size()) {
            best = order.get(req.bestSlot() - 1);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid best model.");
        }
        trial.markEvaluated(best, trimToNull(req.bestReason()));
        trials.save(trial);

        UUID pid = trial.getParticipant().getId();
        long started = trials.countByParticipantId(pid);
        long evaluated = trials.countByParticipantIdAndEvaluatedAtIsNotNull(pid);
        return new EvaluationResult(trial.getId(), recorded, evaluated,
                Math.max(0, props.getMaxTrialsPerParticipant() - started));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static Map<String, Object> irSnapshot(PromptIR ir) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userInput", ir.getUserInput());
        m.put("taskInstruction", ir.getTaskInstruction());
        m.put("role", ir.getRole());
        m.put("detectedRole", ir.getDetectedRole());
        m.put("outputFormat", ir.getOutputFormat());
        m.put("contextBlock", ir.getContextBlock());
        m.put("tokenBudget", ir.getTokenBudget());
        return m;
    }

    private String toJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Throwable rootCause(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        return c;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimToNull(String s) {
        return isBlank(s) ? null : s.strip();
    }
}
