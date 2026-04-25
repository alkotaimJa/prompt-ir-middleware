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
 * T7 — Token Budgeting.
 * <p>
 * Estimates the token count of each compiled prompt using the heuristic
 * {@code characters / 4} and, if any provider's budget would be exceeded,
 * truncates the {@link PromptIR} content fields (context block first, then
 * task instruction) and re-compiles all affected providers. The most
 * restrictive budget in use is stored on the IR for downstream reference.
 */
@Component
@Order(7)
public class TokenBudgetingTransformer implements PromptTransformer {

    private final Map<String, Integer> budgets;
    private final List<ProviderPromptCompiler> compilers;

    public TokenBudgetingTransformer(
            @Value("${pipeline.token.budget.openai:128000}")  int openAiBudget,
            @Value("${pipeline.token.budget.claude:200000}")  int claudeBudget,
            @Value("${pipeline.token.budget.gemini:1000000}") int geminiBudget,
            List<ProviderPromptCompiler> compilers) {
        this.budgets   = Map.of("openai", openAiBudget, "claude", claudeBudget, "gemini", geminiBudget);
        this.compilers = compilers;
    }

    @Override
    public PromptIR transform(PromptIR ir, ChatRequest originalRequest) {
        // Determine effective budget: minimum across all compiled providers, then
        // optionally further constrained by a per-request maxTokens hint.
        int minBudget = ir.getCompiledPrompts().keySet().stream()
                .mapToInt(p -> budgets.getOrDefault(p, Integer.MAX_VALUE))
                .min()
                .orElse(Integer.MAX_VALUE);

        Integer requestMax = originalRequest.getMaxTokens();
        if (requestMax != null && requestMax > 0) {
            minBudget = Math.min(minBudget, requestMax);
        }
        ir.setTokenBudget(minBudget);

        // Check whether any compiled prompt exceeds its provider's budget.
        boolean truncationNeeded = false;
        for (Map.Entry<String, CompiledPrompt> entry : ir.getCompiledPrompts().entrySet()) {
            int providerBudget = budgets.getOrDefault(entry.getKey(), Integer.MAX_VALUE);
            String combinedText = entry.getValue().systemText() + entry.getValue().userText();
            if (estimateTokens(combinedText) > providerBudget) {
                truncationNeeded = true;
                break;
            }
        }

        if (truncationNeeded) {
            applyTruncation(ir, minBudget);
        }

        return ir;
    }

    /**
     * Truncates {@code ir}'s context block first, then the task instruction,
     * until the combined content fits within {@code budgetTokens}.
     * Afterwards all compiled prompts are re-generated.
     */
    private void applyTruncation(PromptIR ir, int budgetTokens) {
        int budgetChars = budgetTokens * 4;

        int totalChars = computeContentChars(ir);
        int excessChars = totalChars - budgetChars;

        if (excessChars <= 0) {
            return;
        }

        // Trim context block first (least critical content).
        String contextBlock = ir.getContextBlock() != null ? ir.getContextBlock() : "";
        if (!contextBlock.isBlank()) {
            int trim = Math.min(excessChars, contextBlock.length());
            ir.setContextBlock(contextBlock.substring(0, contextBlock.length() - trim));
            excessChars -= trim;
        }

        // If still over budget, trim the task instruction suffix.
        if (excessChars > 0) {
            String instruction = ir.getTaskInstruction() != null ? ir.getTaskInstruction() : "";
            if (!instruction.isBlank()) {
                int trim = Math.min(excessChars, instruction.length());
                ir.setTaskInstruction(instruction.substring(0, instruction.length() - trim));
            }
        }

        // Re-compile all providers with the updated IR.
        for (ProviderPromptCompiler compiler : compilers) {
            CompiledPrompt recompiled = compiler.compile(ir);
            ir.getCompiledPrompts().put(recompiled.providerName(), recompiled);
        }
    }

    /** Rough character count of all content fields that end up in the prompt. */
    private int computeContentChars(PromptIR ir) {
        int total = 0;
        if (ir.getRole()            != null) total += ir.getRole().length();
        if (ir.getTaskInstruction() != null) total += ir.getTaskInstruction().length();
        if (ir.getContextBlock()    != null) total += ir.getContextBlock().length();
        if (ir.getUserInput()       != null) total += ir.getUserInput().length();
        return total;
    }

    /** Approximates token count as {@code text.length() / 4}. */
    static int estimateTokens(String text) {
        return (text == null) ? 0 : text.length() / 4;
    }
}
