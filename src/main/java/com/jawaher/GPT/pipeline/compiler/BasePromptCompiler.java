package com.jawaher.GPT.pipeline.compiler;

import com.jawaher.GPT.model.PromptIR;

/**
 * Shared assembly logic for all provider prompt compilers.
 * <p>
 * Subclasses supply a provider-specific system text and inherit the common
 * user-text assembly that concatenates the scaffolded instruction, the
 * delimited context block, and the original user input in that order.
 */
public abstract class BasePromptCompiler implements ProviderPromptCompiler {

    /**
     * Assembles the user-facing portion of the prompt from the three
     * content sections carried by the IR.
     *
     * @param ir fully assembled intermediate representation
     * @return concatenated user text, trailing whitespace stripped
     */
    protected String assembleUserText(PromptIR ir) {
        StringBuilder sb = new StringBuilder();

        if (ir.getTaskInstruction() != null && !ir.getTaskInstruction().isBlank()) {
            sb.append(ir.getTaskInstruction()).append("\n\n");
        }
        if (ir.getContextBlock() != null && !ir.getContextBlock().isBlank()) {
            sb.append(ir.getContextBlock()).append("\n\n");
        }
        // T1 (Instruction Scaffolding) normally embeds the user input inside the
        // task instruction. Appending it again would send the query twice, which
        // confounds the raw-vs-transformed comparison, so it is only appended
        // when the instruction does not already contain it.
        String userInput = ir.getUserInput();
        String instruction = ir.getTaskInstruction();
        boolean alreadyEmbedded = instruction != null && userInput != null
                && !userInput.isBlank() && instruction.contains(userInput.strip());
        if (userInput != null && !userInput.isBlank() && !alreadyEmbedded) {
            sb.append(userInput);
        }

        return sb.toString().stripTrailing();
    }
}
