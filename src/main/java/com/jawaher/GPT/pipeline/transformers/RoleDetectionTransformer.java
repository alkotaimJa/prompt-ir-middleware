package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import com.jawaher.GPT.services.RoleDetectionService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * T4 – Role Detection.
 * <p>
 * Infers a domain-expert persona from the task type and unambiguous domain
 * terms (see {@link RoleDetectionService}). Leaves {@code detectedRole} empty
 * when no persona is justified, because generic personas do not reliably
 * improve responses.
 */
@Component
@Order(40)
public class RoleDetectionTransformer implements PromptTransformer {

    public static final String ID = "role-detection";

    private final RoleDetectionService roleDetectionService;

    public RoleDetectionTransformer(RoleDetectionService roleDetectionService) {
        this.roleDetectionService = roleDetectionService;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest request) {
        String detected = roleDetectionService.detectRole(ir.getUserInput(), ir.getTaskType(), ir.getAudience());
        if (detected != null) {
            ir.setDetectedRole(detected);
        }
        return ir;
    }
}
