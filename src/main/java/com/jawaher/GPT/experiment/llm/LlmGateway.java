package com.jawaher.GPT.experiment.llm;

/**
 * Port through which the experiment calls an LLM provider. Decouples the
 * experiment logic from Spring AI so it can be tested with a stub and so new
 * providers can be added without touching the experiment service.
 */
public interface LlmGateway {

    /**
     * Sends one prompt to a provider and returns the text of the response.
     *
     * @param provider     provider id (openai, claude, gemini)
     * @param systemPrompt system / instruction text; {@code null} or blank for none
     * @param userPrompt   user message
     * @return response text
     * @throws RuntimeException when the provider call fails
     */
    String complete(String provider, String systemPrompt, String userPrompt);

    /** Model identifier used for the provider (stored with every response). */
    String modelFor(String provider);
}
