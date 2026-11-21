package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.model.TaskType;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * T1 – Task Classification.
 * <p>
 * Uses the task category supplied with the request (the participant's choice in
 * the evaluation UI) and otherwise infers the task type from the input with
 * ordered rules. All later stages specialise their output on this type.
 */
@Component
@Order(10)
public class TaskClassificationTransformer implements PromptTransformer {

    public static final String ID = "task-classification";

    private static final Pattern CODING = Pattern.compile(
            "\\b(write|implement|fix|debug|refactor|optimi[sz]e)\\b.*\\b(code|function|method|class|script|program|query|algorithm)\\b"
                    + "|\\b(stack ?trace|traceback|exception|compile error|syntax error)\\b"
                    + "|```",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern COMPARATIVE = Pattern.compile(
            "\\b(compare|comparison|versus|vs\\.?|difference between|differences between|pros and cons|which is better|better than)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INSTRUCTION = Pattern.compile(
            "\\b(list|give me|provide|write|draft|create|generate)\\b.*\\b(\\d+|one|two|three|four|five|six|seven|eight|nine|ten)\\b"
                    + "|\\b(bullet points?|numbered list|in (?:a )?table|as json|exactly)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ANALYTICAL = Pattern.compile(
            "\\b(explain|why|how does|how do|how is|how are|analy[sz]e|reason|implications?|impact of)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FACTUAL = Pattern.compile(
            "^\\s*(what|who|when|where|which|is|are|does|do|can|how many|how much)\\b",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String id() {
        return ID;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest request) {
        TaskType type = TaskType.fromCategory(request.getTaskType());
        if (type == null || type == TaskType.GENERAL) {
            // "Other" (or no category) falls back to rule-based inference.
            type = infer(ir.getUserInput());
        }
        ir.setTaskType(type);
        return ir;
    }

    static TaskType infer(String input) {
        if (input == null || input.isBlank()) {
            return TaskType.GENERAL;
        }
        if (CODING.matcher(input).find()) return TaskType.CODING;
        if (COMPARATIVE.matcher(input).find()) return TaskType.COMPARATIVE;
        if (INSTRUCTION.matcher(input).find()) return TaskType.INSTRUCTION_FOLLOWING;
        if (ANALYTICAL.matcher(input).find()) return TaskType.ANALYTICAL;
        if (FACTUAL.matcher(input).find()) return TaskType.FACTUAL_QA;
        return TaskType.GENERAL;
    }
}
