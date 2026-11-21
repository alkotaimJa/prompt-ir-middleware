package com.jawaher.GPT.pipeline.transformers;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.model.CompiledPrompt;
import com.jawaher.GPT.model.PromptIR;
import com.jawaher.GPT.pipeline.PromptTransformer;
import com.jawaher.GPT.pipeline.compiler.ProviderPromptCompiler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * S3 – Input Token Budgeting (infrastructure guard, always applied).
 * <p>
 * Records the effective input budget (the smallest provider budget, optionally
 * lowered by the request's {@code maxTokens}) and, if a compiled prompt would
 * exceed it, trims the supporting context block and recompiles. The user's own
 * request and the task instruction are never truncated. Tokens are estimated as
 * characters / 4. With current context windows this guard rarely triggers for
 * interactive prompts; it protects against very large pasted context.
 */
@Component
@Order(110)
public class TokenBudgetingTransformer implements PromptTransformer {

    public static final String ID = "token-budgeting";

    private final Map<String, Integer> budgets;
    private final List<ProviderPromptCompiler> compilers;

    public TokenBudgetingTransformer(
            @Value("${pipeline.token.budget.openai:128000}") int openAiBudget,
            @Value("${pipeline.token.budget.claude:200000}") int claudeBudget,
            @Value("${pipeline.token.budget.gemini:1000000}") int geminiBudget,
            List<ProviderPromptCompiler> compilers) {
        this.budgets = Map.of("openai", openAiBudget, "claude", claudeBudget, "gemini", geminiBudget);
        this.compilers = compilers;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean optional() {
        return false;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest request) {
        int budget = ir.getCompiledPrompts().keySet().stream()
                .mapToInt(p -> budgets.getOrDefault(p, Integer.MAX_VALUE))
                .min()
                .orElse(Integer.MAX_VALUE);
        Integer requestMax = request.getMaxTokens();
        if (requestMax != null && requestMax > 0) {
            budget = Math.min(budget, requestMax);
        }
        ir.setTokenBudget(budget);

        int largest = ir.getCompiledPrompts().values().stream()
                .mapToInt(c -> estimateTokens(c.systemText()) + estimateTokens(c.userText()))
                .max()
                .orElse(0);
        int excessTokens = largest - budget;
        String context = ir.getContextBlock() == null ? "" : ir.getContextBlock();
        if (excessTokens > 0 && !context.isEmpty()) {
            int keepChars = Math.max(0, context.length() - excessTokens * 4);
            ir.setContextBlock(context.substring(0, keepChars));
            ir.getProviderHints().put("contextTruncated", true);
            for (ProviderPromptCompiler compiler : compilers) {
                CompiledPrompt recompiled = compiler.compile(ir);
                ir.getCompiledPrompts().put(recompiled.providerName(), recompiled);
            }
        }
        return ir;
    }

    static int estimateTokens(String text) {
        return (text == null) ? 0 : text.length() / 4;
    }
}
