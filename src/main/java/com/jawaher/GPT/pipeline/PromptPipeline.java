package com.jawaher.GPT.pipeline;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Orchestrates the sequential prompt transformation pipeline.
 * <p>
 * A fresh {@link PromptIR} is initialised from the raw user message and passed
 * through every registered {@link PromptTransformer} in {@code @Order} sequence.
 * Optional layers listed in {@code pipeline.disabled-layers} are skipped, and
 * the resulting configuration is exposed as {@link #versionTag()} so every
 * stored experiment trial records exactly which pipeline produced it.
 */
@Service
public class PromptPipeline {

    /** Version of the transformation design. Increment when layer behaviour changes. */
    public static final String VERSION = "2.0";

    private static final Logger log = LoggerFactory.getLogger(PromptPipeline.class);

    private final List<PromptTransformer> transformers;
    private final Set<String> disabled;

    public PromptPipeline(List<PromptTransformer> transformers) {
        this(transformers, "");
    }

    @Autowired
    public PromptPipeline(List<PromptTransformer> transformers,
                          @Value("${pipeline.disabled-layers:}") String disabledLayers) {
        this.transformers = transformers;
        this.disabled = disabledLayers == null ? Collections.emptySet()
                : Arrays.stream(disabledLayers.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Runs the complete transformation pipeline for the given request.
     *
     * @param request the incoming API request
     * @return fully transformed and compiled {@link PromptIR}
     */
    public PromptIR process(ChatRequest request) {
        PromptIR ir = new PromptIR(request.getMessage());
        for (PromptTransformer transformer : transformers) {
            if (transformer.optional() && disabled.contains(transformer.id())) {
                log.debug("Skipping disabled layer: {}", transformer.id());
                continue;
            }
            ir = transformer.transform(ir, request);
            ir.getAppliedLayers().add(transformer.id());
            log.debug("Applied layer {} | task={} role={} format={}",
                    transformer.id(), ir.getTaskType(), ir.getRole(), ir.getOutputFormat());
        }
        return ir;
    }

    /** E.g. {@code "2.0"} or {@code "2.0-without:role-conditioning,role-detection"}. */
    public String versionTag() {
        return disabled.isEmpty() ? VERSION : VERSION + "-without:" + String.join(",", disabled);
    }
}
