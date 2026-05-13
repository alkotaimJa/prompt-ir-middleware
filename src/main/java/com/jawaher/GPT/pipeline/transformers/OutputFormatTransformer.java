package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * T3 — Output Format Specification.
 * <p>
 * Appends a format-directive sentence to the task instruction when a recognised
 * output format is requested explicitly or inferred from the user message.
 * Unknown or absent formats leave the instruction unchanged.
 *
 * <p>Supported format identifiers:
 * <ul>
 *   <li>{@code bullet_points} – bulleted list</li>
 *   <li>{@code json} – valid JSON</li>
 *   <li>{@code numbered_list} – numbered list</li>
 *   <li>{@code prose} – flowing prose</li>
 *   <li>{@code sections} – section headings</li>
 * </ul>
 */
@Component
@Order(4)
public class OutputFormatTransformer implements PromptTransformer {

    private static final Map<String, String> FORMAT_DIRECTIVES = Map.of(
            "bullet_points", "Format your response as a bulleted list.",
            "json", "Format your response as valid JSON.",
            "numbered_list", "Format your response as a numbered list.",
            "prose", "Format your response as clear, flowing prose.",
            "sections", "Format your response with clear section headings."
    );

    private static final Map<String, String> FORMAT_ALIASES = Map.of(
            "bullet", "bullet_points",
            "bullets", "bullet_points",
            "bulleted_list", "bullet_points",
            "numbered", "numbered_list",
            "numberedlist", "numbered_list",
            "numbered_points", "numbered_list"
    );

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest originalRequest) {
        String resolvedFormat = resolveRequestedFormat(originalRequest.getOutputFormat());
        if (resolvedFormat == null) {
            resolvedFormat = detectFormatFromUserInput(ir.getUserInput());
        }

        if (resolvedFormat != null) {
            ir.setOutputFormat(resolvedFormat);

            String directive = FORMAT_DIRECTIVES.get(resolvedFormat);
            String current = ir.getTaskInstruction() != null ? ir.getTaskInstruction() : "";
            if (directive != null && !current.contains(directive)) {
                ir.setTaskInstruction((current + " " + directive).trim());
            }
        }
        return ir;
    }

    private String resolveRequestedFormat(String requestedFormat) {
        if (requestedFormat == null || requestedFormat.isBlank()) {
            return null;
        }

        String normalized = normalizeFormat(requestedFormat);
        if (FORMAT_DIRECTIVES.containsKey(normalized)) {
            return normalized;
        }
        return FORMAT_ALIASES.get(normalized);
    }

    private String detectFormatFromUserInput(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return null;
        }
        String normalized = normalizeText(userInput);

        if (containsWord(normalized, "json")) {
            return "json";
        }
        if (normalized.contains("bullet points") || normalized.contains("bulleted list") || normalized.contains("bullet list")) {
            return "bullet_points";
        }
        if (normalized.contains("numbered list") || normalized.contains("step by step") || normalized.contains("numbered steps")) {
            return "numbered_list";
        }
        if (containsWord(normalized, "sections") || containsWord(normalized, "section")
                || containsWord(normalized, "headings") || containsWord(normalized, "heading")) {
            return "sections";
        }
        if (containsWord(normalized, "prose")
                || containsWord(normalized, "paragraph")
                || containsWord(normalized, "paragraphs")) {
            return "prose";
        }
        return null;
    }

    private String normalizeText(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replaceAll("[^\\p{Alnum}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsWord(String text, String word) {
        return (" " + text + " ").contains(" " + word + " ");
    }

    private String normalizeFormat(String format) {
        return format.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}
