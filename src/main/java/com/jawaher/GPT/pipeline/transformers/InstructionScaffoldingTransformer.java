package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * T1 — Instruction Scaffolding (Task Restatement).
 * <p>
 * Reformulates the raw user input into an explicit, well-defined task
 * instruction by surrounding it with a configurable prefix and suffix.
 * The result is written to {@link PromptIR#setTaskInstruction(String)}.
 */
@Component
@Order(1)
public class InstructionScaffoldingTransformer implements PromptTransformer {

    private final String prefix;
    private final String suffix;

    public InstructionScaffoldingTransformer(
            @Value("${pipeline.scaffolding.prefix:Your task is:}") String prefix,
            @Value("${pipeline.scaffolding.suffix:Please complete this task clearly and accurately.}") String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest originalRequest) {
        String userInput = ir.getUserInput() != null ? ir.getUserInput() : "";
        ir.setTaskInstruction(prefix + " " + userInput + " " + suffix);
        return ir;
    }
}
