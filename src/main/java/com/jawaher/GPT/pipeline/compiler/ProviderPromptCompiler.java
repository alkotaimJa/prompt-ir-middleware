package com.jawaher.GPT.pipeline.compiler;

import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;

/**
 * Strategy interface for compiling a provider-neutral {@link PromptIR} into
 * the native prompt format required by a specific LLM provider.
 */
public interface ProviderPromptCompiler {

    /**
     * Returns the canonical provider name this compiler targets
     * (e.g. {@code "openai"}, {@code "claude"}, {@code "gemini"}).
     */
    String providerName();

    /**
     * Compiles {@code ir} into a {@link CompiledPrompt} ready for the target provider.
     *
     * @param ir fully assembled intermediate representation
     * @return provider-specific compiled prompt
     */
    CompiledPrompt compile(PromptIR ir);
}
