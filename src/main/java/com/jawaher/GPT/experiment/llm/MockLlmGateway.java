package com.jawaher.GPT.experiment.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic stub used for local development, demos and automated tests
 * ({@code experiment.mock-llm=true}). Makes no network calls and costs nothing.
 */
@Component
@ConditionalOnProperty(name = "experiment.mock-llm", havingValue = "true")
public class MockLlmGateway implements LlmGateway {

    @Override
    public String complete(String provider, String systemPrompt, String userPrompt) {
        // Output deliberately never names the provider or the condition, so the
        // blinded UI can be demonstrated and tested exactly as in production.
        boolean structured = systemPrompt != null && !systemPrompt.isBlank();
        int variant = Math.floorMod((provider + userPrompt).hashCode(), 3);
        String[] openers = {"Here is an overview.", "Short answer first, then details.", "Let us break this down."};
        if (structured) {
            return "## Overview\n\n" + openers[variant] + "\n\n"
                    + "1. **Key idea**: the central concept behind the question.\n"
                    + "2. **How it works**: the main mechanism, step by step.\n"
                    + "3. **Example**: a concrete illustration.\n\n"
                    + "_(Mock response: set EXPERIMENT_MOCK_LLM=false and provide API keys for real answers.)_";
        }
        return openers[variant] + " The question touches on a few related points that are worth explaining "
                + "together, with a brief example at the end.\n\n"
                + "(Mock response: set EXPERIMENT_MOCK_LLM=false and provide API keys for real answers.)";
    }

    @Override
    public String modelFor(String provider) {
        return "mock-" + provider;
    }
}
