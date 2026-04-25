package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * T2 — Role Prompting / Persona Framing.
 * <p>
 * Assigns an explicit role to condition model behaviour. If the request
 * carries a non-blank {@code role} field that value is used; otherwise the
 * configurable default role is applied.
 */
@Component
@Order(2)
public class RoleConditioningTransformer implements PromptTransformer {

    private final String defaultRole;

    public RoleConditioningTransformer(
            @Value("${pipeline.role.default:a helpful and precise AI assistant}") String defaultRole) {
        this.defaultRole = defaultRole;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest originalRequest) {
        String requested = originalRequest.getRole();
        ir.setRole((requested != null && !requested.isBlank()) ? requested : defaultRole);
        ir.setOutputFormat(originalRequest.getOutputFormat());
        return ir;
    }
}
