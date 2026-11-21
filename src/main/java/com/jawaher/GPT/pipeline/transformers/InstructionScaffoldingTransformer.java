package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.model.TaskType;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * T3 – Task-Specific Instruction Scaffolding.
 * <p>
 * Replaces the generic "Your task is: …" wrapper of version 1 with an explicit
 * task instruction and success criteria chosen by task type. The user's own
 * words are kept unchanged in {@link PromptIR#getUserInput()} and rendered in a
 * separate, delimited "Request" section by the compilers.
 */
@Component
@Order(30)
public class InstructionScaffoldingTransformer implements PromptTransformer {

    public static final String ID = "instruction-scaffolding";

    record Scaffold(String instruction, List<String> criteria) {
    }

    static final Map<TaskType, Scaffold> SCAFFOLDS = Map.of(
            TaskType.FACTUAL_QA, new Scaffold(
                    "Answer the user's question accurately.",
                    List.of("State the direct answer in the first sentence.",
                            "Follow it with the key facts that support or qualify the answer.")),
            TaskType.ANALYTICAL, new Scaffold(
                    "Explain the topic in the user's request so that the reasoning is easy to follow.",
                    List.of("Start from the core idea, then build up to the details.",
                            "Explain why and how, not only what.",
                            "Include one concrete example.")),
            TaskType.INSTRUCTION_FOLLOWING, new Scaffold(
                    "Complete the user's request exactly as specified.",
                    List.of("Satisfy every explicit requirement, such as the number of items, the format and the scope.",
                            "Do not add content that was not asked for.")),
            TaskType.COMPARATIVE, new Scaffold(
                    "Compare the options in the user's request.",
                    List.of("Identify the criteria that matter most for this comparison.",
                            "Compare the options on each criterion.",
                            "End with a clear verdict stating when each option is the better choice.")),
            TaskType.CODING, new Scaffold(
                    "Solve the programming request in the user's message.",
                    List.of("Provide correct, complete and idiomatic code.",
                            "Briefly explain how the code works.",
                            "Mention important edge cases or limitations.")),
            TaskType.GENERAL, new Scaffold(
                    "Respond to the user's request.",
                    List.of("Address every part of the request.",
                            "Organise the answer logically.")));

    @Override
    public String id() {
        return ID;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest request) {
        TaskType type = ir.getTaskType() == null ? TaskType.GENERAL : ir.getTaskType();
        Scaffold s = SCAFFOLDS.get(type);
        ir.setTaskInstruction(s.instruction());
        ir.getSuccessCriteria().clear();
        ir.getSuccessCriteria().addAll(s.criteria());
        return ir;
    }
}
