package com.jawaher.GPT.pipeline;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.PromptIR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the sequential prompt transformation pipeline.
 * <p>
 * On each invocation a fresh {@link PromptIR} is initialised from the raw
 * user message and passed through every registered {@link PromptTransformer}
 * in {@code @Order} sequence (T1 → T2 → … → T7). The fully populated IR
 * returned at the end contains provider-specific {@link com.jawaher.GPT.model.CompiledPrompt}
 * objects that the service layer uses to drive the actual LLM calls.
 *
 * <p>Spring auto-collects all {@link PromptTransformer} beans into an ordered
 * list, so adding a new transformer only requires declaring it as a
 * {@code @Component} with the appropriate {@code @Order} value.
 */
@Service
public class PromptPipeline {

    private static final Logger log = LoggerFactory.getLogger(PromptPipeline.class);

    private final List<PromptTransformer> transformers;

    public PromptPipeline(List<PromptTransformer> transformers) {
        this.transformers = transformers;
    }

    /**
     * Runs the complete transformation pipeline for the given request.
     *
     * @param request the incoming API request
     * @return fully transformed and compiled {@link PromptIR}
     */
    public PromptIR process(ChatRequest request) {
        PromptIR ir = new PromptIR(request.getMessage());
        for (PromptTransformer transformer : transformers) {
            String stepName = transformer.getClass().getSimpleName();
            log.info("Starting transformation step: {}", stepName);
            ir = transformer.transform(ir, request);
            log.info(
                    "Completed transformation step: {} | role={} | outputFormat={} | tokenBudget={} | compiledPrompts={}",
                    stepName,
                    ir.getRole(),
                    ir.getOutputFormat(),
                    ir.getTokenBudget(),
                    ir.getCompiledPrompts().keySet());
        }
        return ir;
    }
}
