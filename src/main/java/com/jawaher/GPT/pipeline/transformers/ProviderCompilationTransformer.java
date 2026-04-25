package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import com.jawaher.GPT.pipeline.compiler.ProviderPromptCompiler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * T7 — Provider-Specific Prompt Compilation.
 * <p>
 * Iterates over every registered {@link ProviderPromptCompiler} and maps the
 * provider-neutral {@link PromptIR} into each provider's native prompt format.
 * The resulting {@link CompiledPrompt} objects are stored in
 * {@link PromptIR#getCompiledPrompts()}, keyed by provider name.
 */
@Component
@Order(7)
public class ProviderCompilationTransformer implements PromptTransformer {

    private final List<ProviderPromptCompiler> compilers;

    public ProviderCompilationTransformer(List<ProviderPromptCompiler> compilers) {
        this.compilers = compilers;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest originalRequest) {
        for (ProviderPromptCompiler compiler : compilers) {
            CompiledPrompt compiled = compiler.compile(ir);
            ir.getCompiledPrompts().put(compiled.providerName(), compiled);
        }
        return ir;
    }
}
