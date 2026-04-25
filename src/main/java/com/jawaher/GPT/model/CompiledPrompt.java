package com.jawaher.GPT.model;

/**
 * Immutable, provider-specific compiled representation of a {@link PromptIR}.
 * <p>
 * Produced by T6 ({@code ProviderCompilationTransformer}) and consumed by the
 * service layer to drive individual LLM calls via Spring AI's
 * {@code ChatClient}.
 *
 * @param providerName identifier of the target LLM provider (e.g. {@code "openai"})
 * @param systemText   content passed to the provider's system / instruction role
 * @param userText     content passed to the provider's user message slot
 */
public record CompiledPrompt(String providerName, String systemText, String userText) {
}
