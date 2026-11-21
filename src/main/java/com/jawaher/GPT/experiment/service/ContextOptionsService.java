package com.jawaher.GPT.experiment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jawaher.GPT.experiment.ExperimentProperties;
import com.jawaher.GPT.experiment.llm.LlmGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Clarifying-context step (supervisor proposal, adapted for validity).
 * <p>
 * Before the answers are generated, an LLM proposes a few short pieces of
 * context the participant could add (expertise, goal, constraint). The
 * participant picks one or skips. The selected context is then given to
 * <b>both</b> experimental conditions, so it does not confound the raw-vs-
 * transformed comparison. Generation tries the configured providers in order
 * and falls back to a fixed, rule-based list if all of them fail, so a provider
 * outage never blocks the study.
 */
@Service
public class ContextOptionsService {

    private static final Logger log = LoggerFactory.getLogger(ContextOptionsService.class);

    /** System prompt for option generation (also recognised by the mock gateway). */
    public static final String SYSTEM_PROMPT =
            "You help users clarify questions they ask an AI assistant. Given the user's question, propose 3 or 4 "
                    + "short, clearly different statements of background context the user might want to add, such as "
                    + "their level of expertise, their goal, or a constraint. Write each statement in the first person "
                    + "and keep it under 20 words. Do not answer the question. Return only a JSON array of strings.";

    public static final String RULE_BASED = "rule-based";

    static final List<String> FALLBACK_OPTIONS = List.of(
            "I am new to this topic and need the basics first.",
            "I work in software and want practical, real-world guidance.",
            "I need a quick overview to make a decision.",
            "I am studying this for an exam and need precise definitions.");

    private static final int MAX_OPTIONS = 4;
    private static final int MAX_OPTION_CHARS = 160;
    private static final Duration TTL = Duration.ofHours(2);

    /** Options handed out, kept server-side so the stored selection cannot be tampered with. */
    public record Offer(UUID participantId, String prompt, List<String> options, String generator, Instant createdAt) {
    }

    private final LlmGateway llm;
    private final ExperimentProperties props;
    private final Executor executor;
    private final ObjectMapper json;
    private final Map<String, Offer> offers = new ConcurrentHashMap<>();
    private final Random random = new SecureRandom();

    public ContextOptionsService(LlmGateway llm, ExperimentProperties props,
                                 @Qualifier("llmTaskExecutor") Executor executor, ObjectMapper json) {
        this.llm = llm;
        this.props = props;
        this.executor = executor;
        this.json = json;
    }

    /** Generates options and returns the token under which they are stored. */
    public Map.Entry<String, Offer> offer(UUID participantId, String prompt, String taskCategory) {
        evictExpired();
        String user = "Task category: " + (taskCategory == null ? "Other" : taskCategory) + "\n\nQuestion:\n" + prompt;
        List<String> options = null;
        String generator = RULE_BASED;
        for (String provider : props.getContextGeneratorProviders()) {
            try {
                String raw = CompletableFuture
                        .supplyAsync(() -> llm.complete(provider, SYSTEM_PROMPT, user), executor)
                        .get(props.getContextTimeoutSeconds(), TimeUnit.SECONDS);
                List<String> parsed = parse(raw);
                if (parsed.size() >= 2) {
                    options = parsed;
                    generator = provider;
                    break;
                }
                log.warn("Context options from {} could not be parsed; trying next provider", provider);
            } catch (Exception e) {
                log.warn("Context option generation failed for {}: {}", provider, e.toString());
            }
        }
        if (options == null) {
            options = FALLBACK_OPTIONS;
        }
        String token = Long.toHexString(random.nextLong()) + Long.toHexString(random.nextLong());
        Offer offer = new Offer(participantId, prompt, List.copyOf(options), generator, Instant.now());
        offers.put(token, offer);
        return Map.entry(token, offer);
    }

    /** Retrieves (and consumes) an offer, or empty if unknown/expired. */
    public Optional<Offer> take(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Offer o = offers.remove(token);
        if (o == null || o.createdAt().plus(TTL).isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(o);
    }

    /** Extracts a JSON array of strings from a model response (tolerates code fences and prose). */
    List<String> parse(String raw) {
        if (raw == null) {
            return List.of();
        }
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return List.of();
        }
        try {
            JsonNode node = json.readTree(raw.substring(start, end + 1));
            LinkedHashSet<String> out = new LinkedHashSet<>();
            for (JsonNode n : node) {
                if (n.isTextual()) {
                    String s = n.asText().strip().replaceAll("\\s+", " ");
                    if (!s.isEmpty() && s.length() <= MAX_OPTION_CHARS) {
                        out.add(s);
                    }
                }
                if (out.size() == MAX_OPTIONS) break;
            }
            return new ArrayList<>(out);
        } catch (Exception e) {
            return List.of();
        }
    }

    private void evictExpired() {
        Instant cutoff = Instant.now().minus(TTL);
        offers.entrySet().removeIf(e -> e.getValue().createdAt().isBefore(cutoff));
    }
}
