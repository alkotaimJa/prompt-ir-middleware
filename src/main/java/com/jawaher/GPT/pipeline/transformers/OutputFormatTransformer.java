package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.model.TaskType;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * T6 – Output Format Specification.
 * <p>
 * Resolves the output format in priority order: explicit request field, format
 * named in the user's wording, then a default for the task type (a comparison
 * table for comparative tasks, fenced code for coding tasks). The directive is
 * stored separately in the IR and rendered by the compilers.
 */
@Component
@Order(60)
public class OutputFormatTransformer implements PromptTransformer {

    public static final String ID = "output-format";

    static final Map<String, String> FORMAT_DIRECTIVES = Map.of(
            "bullet_points", "Format the answer as a bulleted list.",
            "json", "Format the answer as valid JSON only.",
            "numbered_list", "Format the answer as a numbered list.",
            "prose", "Write the answer as clear, flowing paragraphs.",
            "sections", "Organise the answer under short section headings.",
            "table", "Present the comparison as a Markdown table, followed by a short verdict.",
            "code", "Put all code in fenced code blocks with a language tag.");

    private static final Map<String, String> FORMAT_ALIASES = Map.of(
            "bullet", "bullet_points",
            "bullets", "bullet_points",
            "bulleted_list", "bullet_points",
            "numbered", "numbered_list",
            "numberedlist", "numbered_list",
            "numbered_points", "numbered_list",
            "markdown_table", "table");

    private static final Map<TaskType, String> TASK_DEFAULTS = Map.of(
            TaskType.COMPARATIVE, "table",
            TaskType.CODING, "code");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest request) {
        String format = resolveRequestedFormat(request.getOutputFormat());
        if (format == null) {
            format = detectFormatFromUserInput(ir.getUserInput());
        }
        if (format == null && ir.getTaskType() != null) {
            format = TASK_DEFAULTS.get(ir.getTaskType());
        }
        if (format != null) {
            ir.setOutputFormat(format);
            ir.setFormatDirective(FORMAT_DIRECTIVES.get(format));
        }
        return ir;
    }

    static String resolveRequestedFormat(String requestedFormat) {
        if (requestedFormat == null || requestedFormat.isBlank()) {
            return null;
        }
        String normalized = requestedFormat.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (FORMAT_DIRECTIVES.containsKey(normalized)) {
            return normalized;
        }
        return FORMAT_ALIASES.get(normalized);
    }

    static String detectFormatFromUserInput(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return null;
        }
        String n = " " + userInput.toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replaceAll("[^\\p{Alnum}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim() + " ";

        if (n.contains(" json ")) return "json";
        if (n.contains(" table ") || n.contains(" tabular ")) return "table";
        if (n.contains(" bullet points ") || n.contains(" bullet point ") || n.contains(" bulleted list ")
                || n.contains(" bullet list ") || n.contains(" bullets ")) return "bullet_points";
        if (n.contains(" numbered list ") || n.contains(" step by step ") || n.contains(" numbered steps ")) return "numbered_list";
        if (n.contains(" sections ") || n.contains(" headings ")) return "sections";
        if (n.contains(" prose ") || n.contains(" paragraph ") || n.contains(" paragraphs ")) return "prose";
        return null;
    }
}
