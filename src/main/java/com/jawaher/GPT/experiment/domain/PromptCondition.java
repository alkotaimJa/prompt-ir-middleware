package com.jawaher.GPT.experiment.domain;

/**
 * The independent variable of the experiment.
 * <ul>
 *   <li>{@link #RAW} – the participant's prompt is sent to the provider verbatim,
 *       with no system prompt (baseline).</li>
 *   <li>{@link #TRANSFORMED} – the prompt is passed through the Prompt IR pipeline
 *       (T1–T8) and the provider-specific compiled prompt is sent.</li>
 * </ul>
 */
public enum PromptCondition {
    RAW,
    TRANSFORMED
}
