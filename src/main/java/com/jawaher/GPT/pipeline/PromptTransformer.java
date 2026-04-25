package com.jawaher.GPT.pipeline;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;

/**
 * Single step in the prompt transformation pipeline.
 * <p>
 * Implementations apply one category of the prompt transformation taxonomy to
 * the supplied {@link PromptIR} and return the (possibly mutated) IR for the
 * next step. Ordering is controlled via Spring's {@code @Order} annotation on
 * each implementing class.
 */
public interface PromptTransformer {

    /**
     * Applies this transformation step to {@code ir}.
     *
     * @param ir              the intermediate representation accumulated so far
     * @param originalRequest the original API request, available for read-only reference
     * @return the updated {@code PromptIR} (may be the same instance, mutated in place)
     */
    PromptIR transform(PromptIR ir, ChatRequest originalRequest);
}
