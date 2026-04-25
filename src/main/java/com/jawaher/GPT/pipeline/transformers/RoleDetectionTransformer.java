package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import com.jawaher.GPT.services.RoleDetectionService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * T2 — Automatic Role Detection.
 * <p>
 * Analyses the raw user message and, when a confident match is found, writes an
 * inferred persona string to {@link PromptIR#setDetectedRole(String)}.  The
 * actual role resolution (explicit &gt; detected &gt; default) is performed by
 * the next step, T3 ({@link RoleConditioningTransformer}).
 * <p>
 * When no pattern matches (or when detection is disabled via
 * {@code pipeline.role.detection.enabled=false}), {@code detectedRole} is left
 * {@code null} so the downstream transformer can distinguish "no detection" from
 * an empty string.
 */
@Component
@Order(2)
public class RoleDetectionTransformer implements PromptTransformer {

    private final RoleDetectionService roleDetectionService;

    public RoleDetectionTransformer(RoleDetectionService roleDetectionService) {
        this.roleDetectionService = roleDetectionService;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest originalRequest) {
        String detected = roleDetectionService.detectRole(ir.getUserInput());
        if (detected != null) {
            ir.setDetectedRole(detected);
        }
        return ir;
    }
}
