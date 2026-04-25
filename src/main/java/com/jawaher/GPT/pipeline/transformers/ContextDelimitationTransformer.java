package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * T4 — Context Delimitation.
 * <p>
 * Wraps the optional background context supplied in the request with clear
 * delimiters, preventing it from bleeding into the instruction or user-input
 * sections of the final prompt. When no context is provided the context block
 * on the IR is left as {@code null}.
 */
@Component
@Order(4)
public class ContextDelimitationTransformer implements PromptTransformer {

    private static final String CONTEXT_START = "### CONTEXT START ###";
    private static final String CONTEXT_END   = "### CONTEXT END ###";

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest originalRequest) {
        String context = originalRequest.getContext();
        if (context != null && !context.isBlank()) {
            ir.setContextBlock(CONTEXT_START + "\n" + context.strip() + "\n" + CONTEXT_END);
        }
        return ir;
    }
}
