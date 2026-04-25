package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * T3 — Role Prompting / Persona Framing.
 * <p>
 * Resolves the final persona string using a three-level priority chain:
 * <ol>
 *   <li><b>Explicit role</b> from {@code ChatRequest.role} — caller-supplied, highest priority.</li>
 *   <li><b>Detected role</b> from {@link PromptIR#getDetectedRole()} — inferred by T2.</li>
 *   <li><b>Default role</b> from {@code pipeline.role.default} configuration — fallback.</li>
 * </ol>
 */
@Component
@Order(3)
public class RoleConditioningTransformer implements PromptTransformer {

    private final String defaultRole;

    public RoleConditioningTransformer(
            @Value("${pipeline.role.default:a helpful and precise AI assistant}") String defaultRole) {
        this.defaultRole = defaultRole;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest originalRequest) {
        String requested = originalRequest.getRole();
        String resolved;
        if (requested != null && !requested.isBlank()) {
            // Priority 1: caller-supplied explicit role
            resolved = requested;
        } else if (ir.getDetectedRole() != null && !ir.getDetectedRole().isBlank()) {
            // Priority 2: auto-detected role from message content
            resolved = ir.getDetectedRole();
        } else {
            // Priority 3: configured default
            resolved = defaultRole;
        }
        ir.setRole(resolved);
        ir.setOutputFormat(originalRequest.getOutputFormat());
        return ir;
    }
}
