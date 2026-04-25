package com.jawaher.GPT.pipeline.compiler;

import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import org.springframework.stereotype.Component;

/**
 * Compiles a {@link PromptIR} into the Anthropic (Claude) prompt format.
 * <p>
 * Maps {@code role} to a system message with Anthropic-style guidance, and
 * assembles the remaining sections into a user message.
 */
@Component
public class AnthropicPromptCompiler extends BasePromptCompiler {

    public static final String PROVIDER_NAME = "claude";

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public CompiledPrompt compile(PromptIR ir) {
        String systemText = "You are " + ir.getRole() + ". "
                + "Provide a thoughtful, nuanced, and well-reasoned response.";
        return new CompiledPrompt(PROVIDER_NAME, systemText, assembleUserText(ir));
    }
}
