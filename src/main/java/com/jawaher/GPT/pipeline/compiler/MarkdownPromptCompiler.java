package com.jawaher.GPT.pipeline.compiler;

/**
 * Renders sections as Markdown headings (used for OpenAI and Gemini, whose
 * prompting guides recommend Markdown or similar delimiters).
 */
public abstract class MarkdownPromptCompiler extends BasePromptCompiler {

    @Override
    protected String section(String title, String tag, String body) {
        return "## " + title + "\n" + body;
    }
}
