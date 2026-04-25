package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * T3 — Output Format Specification.
 * <p>
 * Appends a format-directive sentence to the task instruction when the request
 * specifies a recognised output format. Unknown or absent formats leave the
 * instruction unchanged.
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
@Order(3)
public class OutputFormatTransformer implements PromptTransformer {

    private static final Map<String, String> FORMAT_DIRECTIVES = Map.of(
            "bullet_points",  "Format your response as a bulleted list.",
            "json",           "Format your response as valid JSON.",
            "numbered_list",  "Format your response as a numbered list.",
            "prose",          "Format your response as clear, flowing prose.",
            "sections",       "Format your response with clear section headings."
    );

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest originalRequest) {
        String format = originalRequest.getOutputFormat();
        if (format != null) {
            String directive = FORMAT_DIRECTIVES.get(format);
            if (directive != null) {
                String current = ir.getTaskInstruction() != null ? ir.getTaskInstruction() : "";
                ir.setTaskInstruction(current + " " + directive);
            }
        }
        return ir;
    }
}
