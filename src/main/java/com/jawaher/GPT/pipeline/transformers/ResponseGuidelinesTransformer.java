package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.model.TaskType;
import com.jawaher.GPT.pipeline.PromptTransformer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * T9 – Response Guidelines.
 * <p>
 * Adds short, task-dependent quality directives:
 * <ul>
 *   <li><b>Assumptions and uncertainty</b> – state assumptions for ambiguous
 *       requests and flag uncertain facts instead of guessing.</li>
 *   <li><b>Planning</b> – for comparative and analytical tasks, decide the key
 *       points before writing, while presenting only the final answer (a light
 *       form of reasoning scaffolding that keeps the output readable).</li>
 *   <li><b>Self-check</b> – for instruction-following tasks, verify that every
 *       explicit requirement is met.</li>
 * </ul>
 */
@Component
@Order(80)
public class ResponseGuidelinesTransformer implements PromptTransformer {

    public static final String ID = "response-guidelines";

    static final String ASSUMPTIONS = "If the request is ambiguous, state the assumption you are making.";
    static final String UNCERTAINTY = "If you are not sure about a fact, say so instead of guessing.";
    static final String PLANNING = "Decide which points matter most before writing, but present only the final, well-organised answer.";
    static final String SELF_CHECK = "Before finishing, check that every explicit requirement in the request is met.";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest request) {
        TaskType type = ir.getTaskType() == null ? TaskType.GENERAL : ir.getTaskType();
        List<String> g = switch (type) {
            case FACTUAL_QA -> List.of(ASSUMPTIONS, UNCERTAINTY);
            case ANALYTICAL, COMPARATIVE -> List.of(ASSUMPTIONS, UNCERTAINTY, PLANNING);
            case INSTRUCTION_FOLLOWING -> List.of(SELF_CHECK);
            case CODING -> List.of(ASSUMPTIONS);
            case GENERAL -> List.of(ASSUMPTIONS, UNCERTAINTY);
        };
        ir.getResponseGuidelines().addAll(g);
        return ir;
    }
}
