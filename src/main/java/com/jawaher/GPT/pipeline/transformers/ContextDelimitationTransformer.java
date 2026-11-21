package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * T8 – Context Delimitation.
 * <p>
 * Places supporting context supplied with the request (in the experiment: the
 * clarifying context the participant selected) in its own IR field. The
 * compilers render it in a dedicated, delimited section (XML tags for Claude,
 * a Markdown heading for the other providers), separate from the task and the
 * user's request.
 */
@Component
@Order(70)
public class ContextDelimitationTransformer implements PromptTransformer {

    public static final String ID = "context-delimitation";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest request) {
        String context = request.getContext();
        if (context != null && !context.isBlank()) {
            ir.setContextBlock(context.strip());
        }
        return ir;
    }
}
