package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
            "bullet_points",  "Format your response as a bulleted list.",
            "json",           "Format your response as valid JSON.",
            "numbered_list",  "Format your response as a numbered list.",
            "prose",          "Format your response as clear, flowing prose.",
            "sections",       "Format your response with clear section headings."
    );
    private record FormatPattern(Pattern pattern, String format) {}
    private static final List<FormatPattern> DETECTION_PATTERNS = List.of(
            new FormatPattern(Pattern.compile("\\bjson\\b", Pattern.CASE_INSENSITIVE), "json"),
            new FormatPattern(Pattern.compile("\\b(bullet\\s*points?|bulleted\\s*list|bullet\\s*list)\\b", Pattern.CASE_INSENSITIVE), "bullet_points"),
            new FormatPattern(Pattern.compile("\\b(numbered\\s*list|step\\s*-?\\s*by\\s*-?\\s*step|steps?)\\b", Pattern.CASE_INSENSITIVE), "numbered_list"),
            new FormatPattern(Pattern.compile("\\b(section(?:s)?|headings?)\\b", Pattern.CASE_INSENSITIVE), "sections"),
            new FormatPattern(Pattern.compile("\\b(prose|paragraphs?)\\b", Pattern.CASE_INSENSITIVE), "prose")
    );

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest originalRequest) {
        String format = resolveFormat(originalRequest, ir);
        String directive = FORMAT_DIRECTIVES.get(format);
        if (directive != null) {
            String current = ir.getTaskInstruction() != null ? ir.getTaskInstruction() : "";
            ir.setTaskInstruction(current + " " + directive);
        }
        return ir;
    }

    private String resolveFormat(ChatRequest originalRequest, PromptIR ir) {
        String explicit = originalRequest.getOutputFormat();
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        String userInput = ir.getUserInput();
        if (userInput == null || userInput.isBlank()) {
            return null;
        }
        for (FormatPattern fp : DETECTION_PATTERNS) {
            if (fp.pattern().matcher(userInput).find()) {
                return fp.format();
            }
        }
        return null;
    }
}
