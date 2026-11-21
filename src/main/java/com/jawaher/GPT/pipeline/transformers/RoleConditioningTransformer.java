package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * T5 – Role Conditioning.
 * <p>
 * Resolves the final persona: an explicit role from the request, then the
 * detected role, then the configured default. The default is empty, so no
 * generic "helpful assistant" persona is added.
 */
@Component
@Order(50)
public class RoleConditioningTransformer implements PromptTransformer {

    public static final String ID = "role-conditioning";

    private final String defaultRole;

    public RoleConditioningTransformer(@Value("${pipeline.role.default:}") String defaultRole) {
        this.defaultRole = defaultRole == null ? "" : defaultRole.trim();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest request) {
        String requested = request.getRole();
        if (requested != null && !requested.isBlank()) {
            ir.setRole(requested.trim());
        } else if (ir.getDetectedRole() != null && !ir.getDetectedRole().isBlank()) {
            ir.setRole(ir.getDetectedRole());
        } else {
            ir.setRole(defaultRole);
        }
        return ir;
    }
}
