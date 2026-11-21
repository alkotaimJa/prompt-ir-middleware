package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * T7 – Length Control.
 * <p>
 * Human and LLM judges tend to prefer longer answers regardless of quality.
 * Without a guard, a transformed prompt could "win" simply by producing more
 * text. This layer adds a proportionality instruction unless the user already
 * stated an explicit length or count constraint (detected by T2).
 */
@Component
@Order(65)
public class LengthControlTransformer implements PromptTransformer {

    public static final String ID = "length-control";

    private final String guidance;

    public LengthControlTransformer(
            @Value("${pipeline.length.guidance:Make the answer as long as the task requires and no longer; avoid filler, repetition and unnecessary introductions.}")
            String guidance) {
        this.guidance = guidance;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest request) {
        if (!ir.isExplicitLengthConstraint()) {
            ir.setLengthGuidance(guidance);
        }
        return ir;
    }
}
