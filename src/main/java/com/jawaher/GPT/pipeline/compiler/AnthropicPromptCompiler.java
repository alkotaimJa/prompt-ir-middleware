package com.jawaher.GPT.pipeline.compiler;

import org.springframework.stereotype.Component;

/**
 * Compiles a Prompt IR for Anthropic Claude, delimiting sections with XML tags
 * as recommended in Anthropic's prompting guidance.
 */
@Component
public class AnthropicPromptCompiler extends BasePromptCompiler {

    public static final String PROVIDER_NAME = "claude";

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    protected String section(String title, String tag, String body) {
        return "<" + tag + ">\n" + body + "\n</" + tag + ">";
    }
}
