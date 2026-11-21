package com.jawaher.GPT.experiment.domain;

/**
 * Un-blinded outcome of a vote: which prompt condition the participant preferred.
 */
public enum Preference {
    RAW,
    TRANSFORMED,
    /** Both responses judged equally good ("no clear preference"). */
    TIE,
    /** Both responses judged unsatisfactory. */
    NEITHER;

    public static Preference resolve(VoteChoice choice, boolean rawIsA) {
        return switch (choice) {
            case A_BETTER -> rawIsA ? RAW : TRANSFORMED;
            case B_BETTER -> rawIsA ? TRANSFORMED : RAW;
            case BOTH_GOOD -> TIE;
            case BOTH_BAD -> NEITHER;
        };
    }
}
