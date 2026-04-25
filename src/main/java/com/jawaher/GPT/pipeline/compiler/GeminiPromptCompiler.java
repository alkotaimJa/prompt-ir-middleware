package com.jawaher.GPT.pipeline.compiler;

import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import org.springframework.stereotype.Component;

/**
 * Compiles a {@link PromptIR} into the Google Gemini prompt format.
 * <p>
 * Spring AI maps the system text to Gemini's {@code system_instruction} field
 * and the user text to the {@code contents} field internally, so this compiler
 * produces the same structural split (system / user) as the other compilers.
 */
@Component
public class GeminiPromptCompiler extends BasePromptCompiler {

    public static final String PROVIDER_NAME = "gemini";

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public CompiledPrompt compile(PromptIR ir) {
        String systemText = "You are " + ir.getRole() + ". "
                + "Provide an accurate, informative, and detailed response.";
        return new CompiledPrompt(PROVIDER_NAME, systemText, assembleUserText(ir));
    }
}
