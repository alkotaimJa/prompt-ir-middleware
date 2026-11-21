package com.jawaher.GPT.pipeline.compiler;

import org.springframework.stereotype.Component;

/** Compiles a Prompt IR for OpenAI chat models (system message + Markdown-sectioned user message). */
@Component
public class OpenAIPromptCompiler extends MarkdownPromptCompiler {

    public static final String PROVIDER_NAME = "openai";

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }
}
