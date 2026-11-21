package com.jawaher.GPT.experiment.domain;

/**
 * What the participant actually clicked. Participants only ever see the blinded
 * labels "Response A" and "Response B"; the mapping to {@link PromptCondition}
 * is resolved server-side into {@link Preference}.
 */
public enum VoteChoice {
    A_BETTER,
    B_BETTER,
    BOTH_GOOD,
    BOTH_BAD
}
