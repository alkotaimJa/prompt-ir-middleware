package com.jawaher.GPT.pipeline.compiler;

import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import org.springframework.stereotype.Component;

/**
 * Compiles a {@link PromptIR} into the OpenAI prompt format.
 * <p>
 * Maps {@code role} to a system message and assembles the task instruction,
 * context block, and user input into a single user message.
 */
@Component
public class OpenAIPromptCompiler extends BasePromptCompiler {

    public static final String PROVIDER_NAME = "openai";

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public CompiledPrompt compile(PromptIR ir) {
        String systemText = "You are " + ir.getRole() + ".";
        return new CompiledPrompt(PROVIDER_NAME, systemText, assembleUserText(ir));
    }
}
