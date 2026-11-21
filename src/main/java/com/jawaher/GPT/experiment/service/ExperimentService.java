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
    private final ContextOptionsService contextOptions;
    private final Random random = new SecureRandom();

    public ExperimentService(PromptPipeline pipeline,
                             LlmGateway llm,
                             ParticipantRepository participants,
                             TrialRepository trials,
                             ExperimentProperties props,
                             @Qualifier("llmTaskExecutor") Executor executor,
                             ObjectMapper json,
                             RateLimiter rateLimiter,
                             ContextOptionsService contextOptions) {
        this.pipeline = pipeline;
        this.llm = llm;
        this.participants = participants;
        this.trials = trials;
        this.props = props;
        this.executor = executor;
        this.json = json;
        this.rateLimiter = rateLimiter;
        this.contextOptions = contextOptions;
    }

    // ── Study info & participants ───────────────────────────────────────────

    public StudyInfo studyInfo() {
        return new StudyInfo(!isBlank(props.getAccessCode()), TASK_CATEGORIES,
                props.getMaxPromptChars(), props.getMinReasonChars(), props.isContextStepEnabled(), props.isMockLlm());
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
                trials.countByParticipantIdAndUsableTrue(id),
                trials.countByParticipantIdAndEvaluatedAtIsNotNull(id),
                props.getMaxTrialsPerParticipant());
    }

    // ── Trials ──────────────────────────────────────────────────────────────

    // ── Clarifying context (optional step before a trial) ──────────────────

    public ContextOptionsView contextOptions(ContextOptionsRequest req, String clientIp) {
        if (!props.isContextStepEnabled()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "The context step is disabled for this study.");
        }
        String prompt = validatePrompt(req.prompt());
        Participant participant = participants.findById(req.participantId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown participant. Please register again."));
        ensureUnderCap(participant);
        if (!rateLimiter.tryAcquire("ctx:" + clientIp)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.");
        }
        Map.Entry<String, ContextOptionsService.Offer> offer =
                contextOptions.offer(participant.getId(), prompt, normaliseCategory(req.taskCategory()));
        return new ContextOptionsView(offer.getKey(), offer.getValue().options());
    }

    // ── Trials ──────────────────────────────────────────────────────────────

    public TrialView startTrial(StartTrialRequest req, String clientIp) {
        String prompt = validatePrompt(req.prompt());
        Participant participant = participants.findById(req.participantId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown participant. Please register again."));
        long usableSoFar = ensureUnderCap(participant);
        if (!rateLimiter.tryAcquire(clientIp)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.");
        }

        String category = normaliseCategory(req.taskCategory());
        Trial trial = new Trial(participant, category, prompt);

        // 0) Clarifying context: resolved from the server-side offer, applied to BOTH conditions.
        String context = null;
        if (req.contextToken() != null && !req.contextToken().isBlank()) {
            ContextOptionsService.Offer offer = contextOptions.take(req.contextToken())
                    .filter(o -> o.participantId().equals(participant.getId()) && o.prompt().equals(prompt))
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                            "The context options have expired. Please submit your question again."));
            trial.setContextOptionsJson(toJson(offer.options()));
            trial.setContextGenerator(offer.generator());
            Integer idx = req.selectedContextIndex();
            if (idx != null && idx >= 0) {
                if (idx >= offer.options().size()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid context option.");
                }
                context = offer.options().get(idx);
            }
        }
        trial.setSelectedContext(context);

        // 1) Transformation: raw prompt → Prompt IR → provider-specific compiled prompts.
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMessage(prompt);
        chatRequest.setTaskType(category);
        chatRequest.setContext(context);
        PromptIR ir = pipeline.process(chatRequest);
        trial.setPipelineVersion(pipeline.versionTag());
        trial.setPromptIrJson(toJson(irSnapshot(ir)));
        trial.setTransformedPromptsJson(toJson(ir.getCompiledPrompts()));

        // The raw condition receives the same information, as plain text the way a user would add it.
        String rawUserPrompt = context == null ? prompt : prompt + "\n\n" + context;

        // 2) Blinding: shuffle provider order and A/B position per provider.
        List<String> providers = new ArrayList<>(props.getProviders());
        Collections.shuffle(providers, random);
        trial.setProviderDisplayOrder(String.join(",", providers));

        List<CompletableFuture<Void>> calls = new ArrayList<>();
        for (String provider : providers) {
            CompiledPrompt compiled = ir.getCompiledPrompts().get(provider);
            boolean rawIsA = random.nextBoolean();
            LlmResponse raw = new LlmResponse(trial, provider, llm.modelFor(provider), PromptCondition.RAW,
                    null, rawUserPrompt, rawIsA ? "A" : "B");
            LlmResponse transformed = new LlmResponse(trial, provider, llm.modelFor(provider), PromptCondition.TRANSFORMED,
                    compiled == null ? null : compiled.systemText(),
                    compiled == null ? prompt : compiled.userText(), rawIsA ? "B" : "A");
            trial.addResponse(raw);
            trial.addResponse(transformed);
            calls.add(execute(raw));
            if (compiled == null) {
                log.error("No prompt compiler registered for provider {}", provider);
                transformed.recordFailure("No prompt compiler registered for provider " + provider, 0);
            } else {
                calls.add(execute(transformed));
            }
        }

        // 3) Execute all calls concurrently. Each call handles its own failure or timeout,
        //    so one provider returning HTTP 500 never affects the others.
        CompletableFuture.allOf(calls.toArray(CompletableFuture[]::new)).join();

        boolean anyVotable = providers.stream().anyMatch(p -> votable(trial, p));
        trial.setUsable(anyVotable);
        trials.save(trial);
        return toView(trial, providers, anyVotable ? usableSoFar + 1 : usableSoFar);
    }

    private String validatePrompt(String raw) {
        String prompt = raw == null ? "" : raw.strip();
        if (prompt.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please enter a prompt.");
        }
        if (prompt.length() > props.getMaxPromptChars()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Prompt is too long (max " + props.getMaxPromptChars() + " characters).");
        }
        return prompt;
    }

    /** Returns the number of usable trials so far; throws when the participant reached the cap. */
    private long ensureUnderCap(Participant participant) {
        long usable = trials.countByParticipantIdAndUsableTrue(participant.getId());
        if (usable >= props.getMaxTrialsPerParticipant()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "You have reached the maximum number of prompts for this study. Thank you!");
        }
        return usable;
    }

    private static String normaliseCategory(String category) {
        // List.of(...).contains(null) throws, so null is checked first.
        return category != null && TASK_CATEGORIES.contains(category) ? category : "Other";
    }

    private static boolean votable(Trial trial, String provider) {
        return cell(trial, provider, "A").isSuccess() && cell(trial, provider, "B").isSuccess();
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

    private TrialView toView(Trial trial, List<String> providerOrder, long usableSoFar) {
        List<ModelPair> pairs = new ArrayList<>();
        for (int i = 0; i < providerOrder.size(); i++) {
            String provider = providerOrder.get(i);
            LlmResponse a = cell(trial, provider, "A");
            LlmResponse b = cell(trial, provider, "B");
            boolean votable = a.isSuccess() && b.isSuccess();
            pairs.add(new ModelPair(i + 1, "Model " + (i + 1), votable, blind(a), blind(b)));
        }
        long remaining = Math.max(0, props.getMaxTrialsPerParticipant() - usableSoFar);
        return new TrialView(trial.getId(), trial.getRawPrompt(), trial.getSelectedContext(), pairs,
                trial.isUsable(), remaining);
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
        if (!trial.isUsable()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "None of the models answered this question, so there is nothing to rate.");
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
        } else if (req.bestSlot() <= order.size() && votable(trial, order.get(req.bestSlot() - 1))) {
            best = order.get(req.bestSlot() - 1);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid best model.");
        }
        trial.markEvaluated(best, trimToNull(req.bestReason()));
        trials.save(trial);

        UUID pid = trial.getParticipant().getId();
        long started = trials.countByParticipantIdAndUsableTrue(pid);
        long evaluated = trials.countByParticipantIdAndEvaluatedAtIsNotNull(pid);
        return new EvaluationResult(trial.getId(), recorded, evaluated,
                Math.max(0, props.getMaxTrialsPerParticipant() - started));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static Map<String, Object> irSnapshot(PromptIR ir) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userInput", ir.getUserInput());
        m.put("taskType", ir.getTaskType() == null ? null : ir.getTaskType().name());
        m.put("audience", ir.getAudience());
        m.put("constraints", ir.getConstraints());
        m.put("taskInstruction", ir.getTaskInstruction());
        m.put("successCriteria", ir.getSuccessCriteria());
        m.put("detectedRole", ir.getDetectedRole());
        m.put("role", ir.getRole());
        m.put("outputFormat", ir.getOutputFormat());
        m.put("formatDirective", ir.getFormatDirective());
        m.put("lengthGuidance", ir.getLengthGuidance());
        m.put("contextBlock", ir.getContextBlock());
        m.put("responseGuidelines", ir.getResponseGuidelines());
        m.put("tokenBudget", ir.getTokenBudget());
        m.put("appliedLayers", ir.getAppliedLayers());
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
