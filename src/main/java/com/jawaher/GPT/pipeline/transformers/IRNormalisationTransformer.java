package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.model.TaskType;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * S1 – IR Normalisation (infrastructure, always applied).
 * <p>
 * Replaces missing values with neutral defaults so the compilers never see
 * {@code null}, including when optional content layers have been disabled.
 */
@Component
@Order(90)
public class IRNormalisationTransformer implements PromptTransformer {

    public static final String ID = "ir-normalisation";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean optional() {
        return false;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest request) {
        if (ir.getUserInput() == null) ir.setUserInput("");
        if (ir.getTaskType() == null) ir.setTaskType(TaskType.GENERAL);
        if (ir.getTaskInstruction() == null) ir.setTaskInstruction("");
        if (ir.getAudience() == null) ir.setAudience("");
        if (ir.getDetectedRole() == null) ir.setDetectedRole("");
        if (ir.getRole() == null) ir.setRole("");
        if (ir.getFormatDirective() == null) ir.setFormatDirective("");
        if (ir.getLengthGuidance() == null) ir.setLengthGuidance("");
        if (ir.getContextBlock() == null) ir.setContextBlock("");
        return ir;
    }
}
