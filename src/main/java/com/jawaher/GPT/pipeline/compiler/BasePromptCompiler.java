package com.jawaher.GPT.pipeline.compiler;

import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared rendering logic for all provider compilers.
 * <p>
 * All providers receive <b>identical wording</b>; compilers differ only in how
 * sections are delimited ({@link #section(String, String, String)}). Earlier
 * versions appended a different quality adjective per provider ("nuanced",
 * "detailed"), which could change answer length between providers and confound
 * the comparison; that has been removed.
 *
 * <pre>
 * system: [You are {role}.] + "Response guidelines:" + format / length / guidelines
 * user:   Task (instruction + success criteria)
 *         Audience and constraints
 *         Context
 *         Request (the user's own words, verbatim)
 * </pre>
 */
public abstract class BasePromptCompiler implements ProviderPromptCompiler {

    /** Renders one named section, e.g. XML tags or a Markdown heading. */
    protected abstract String section(String title, String tag, String body);

    @Override
    public CompiledPrompt compile(PromptIR ir) {
        return new CompiledPrompt(providerName(), systemText(ir), userText(ir));
    }

    protected String systemText(PromptIR ir) {
        StringBuilder sb = new StringBuilder();
        if (notBlank(ir.getRole())) {
            sb.append("You are ").append(ir.getRole().strip()).append('.');
        }
        List<String> guidelines = new ArrayList<>();
        if (notBlank(ir.getFormatDirective())) guidelines.add(ir.getFormatDirective());
        if (notBlank(ir.getLengthGuidance())) guidelines.add(ir.getLengthGuidance());
        guidelines.addAll(ir.getResponseGuidelines());
        if (!guidelines.isEmpty()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("Response guidelines:\n").append(bullets(guidelines));
        }
        return sb.toString().strip();
    }

    protected String userText(PromptIR ir) {
        List<String> parts = new ArrayList<>();

        StringBuilder task = new StringBuilder();
        if (notBlank(ir.getTaskInstruction())) task.append(ir.getTaskInstruction().strip());
        if (!ir.getSuccessCriteria().isEmpty()) {
            if (task.length() > 0) task.append('\n');
            task.append(bullets(ir.getSuccessCriteria()));
        }
        if (task.length() > 0) parts.add(section("Task", "task", task.toString()));

        List<String> constraints = new ArrayList<>();
        if (notBlank(ir.getAudience())) constraints.add("Audience: " + ir.getAudience().strip() + ".");
        constraints.addAll(ir.getConstraints());
        if (!constraints.isEmpty()) parts.add(section("Audience and constraints", "constraints", bullets(constraints)));

        if (notBlank(ir.getContextBlock())) {
            parts.add(section("Context provided by the user", "context", ir.getContextBlock().strip()));
        }

        String request = ir.getUserInput() == null ? "" : ir.getUserInput().strip();
        // With every content layer disabled the compiled prompt degenerates to the raw request.
        parts.add(parts.isEmpty() ? request : section("Request", "request", request));
        return String.join("\n\n", parts);
    }

    protected static String bullets(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String i : items) {
            if (sb.length() > 0) sb.append('\n');
            sb.append("- ").append(i.strip());
        }
        return sb.toString();
    }

    protected static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
