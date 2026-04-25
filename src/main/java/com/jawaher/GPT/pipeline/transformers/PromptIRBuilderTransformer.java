package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * T5 — Prompt IR Assembly (structural finalisation).
 * <p>
 * Ensures the {@link PromptIR} is fully populated before provider compilation.
 * Any field that is still {@code null} after T1–T4 is defaulted to an empty
 * string, guaranteeing downstream transformers and compilers never encounter
 * {@code null} on mandatory fields.
 */
@Component
@Order(5)
public class PromptIRBuilderTransformer implements PromptTransformer {

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest originalRequest) {
        if (ir.getUserInput() == null) {
            ir.setUserInput("");
        }
        if (ir.getTaskInstruction() == null) {
            ir.setTaskInstruction(ir.getUserInput());
        }
        if (ir.getRole() == null) {
            ir.setRole("");
        }
        if (ir.getContextBlock() == null) {
            ir.setContextBlock("");
        }
        return ir;
    }
}
