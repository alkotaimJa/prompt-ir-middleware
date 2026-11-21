package com.jawaher.GPT.pipeline;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;

/**
 * Single step in the prompt transformation pipeline.
 * <p>
 * Content transformations (T1–T9) apply one prompting technique and can be
 * switched off individually through {@code pipeline.disabled-layers}, which
 * supports ablation studies. Infrastructure stages (S1–S3: normalisation,
 * provider compilation, token budgeting) are always applied. Ordering is
 * controlled with Spring's {@code @Order}.
 */
public interface PromptTransformer {

    /** Stable identifier used in configuration, logs and the stored pipeline version. */
    String id();

    /** Whether this layer may be disabled (true for content transformations). */
    default boolean optional() {
        return true;
    }

    /**
     * Applies this transformation step to {@code ir}.
     *
     * @param ir              the intermediate representation accumulated so far
     * @param originalRequest the original API request, available for read-only reference
     * @return the updated {@code PromptIR} (may be the same instance, mutated in place)
     */
    PromptIR transform(PromptIR ir, ChatRequest originalRequest);
}
