package com.jawaher.GPT.pipeline.compiler;

import org.springframework.stereotype.Component;

/**
 * Compiles a Prompt IR for Google Gemini. Spring AI maps the system text to
 * Gemini's {@code system_instruction} and the user text to {@code contents}.
 */
@Component
public class GeminiPromptCompiler extends MarkdownPromptCompiler {

    public static final String PROVIDER_NAME = "gemini";

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }
}
