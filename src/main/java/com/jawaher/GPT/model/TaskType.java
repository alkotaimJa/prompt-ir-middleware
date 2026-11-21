package com.jawaher.GPT.model;

import java.util.Locale;

/**
 * Task taxonomy used by the pipeline to select task-specific instructions,
 * default output formats and response guidelines.
 */
public enum TaskType {
    FACTUAL_QA("Factual QA"),
    ANALYTICAL("Analytical explanation"),
    INSTRUCTION_FOLLOWING("Instruction following"),
    COMPARATIVE("Comparative reasoning"),
    CODING("Coding"),
    GENERAL("Other");

    private final String label;

    TaskType(String label) {
        this.label = label;
    }

    /** Label shown to participants in the evaluation UI. */
    public String label() {
        return label;
    }

    /**
     * Maps a participant-facing category label (or an enum name) to a task type.
     *
     * @return the matching type, or {@code null} when the value is blank or unknown
     */
    public static TaskType fromCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        for (TaskType t : values()) {
            if (t.label.equalsIgnoreCase(v) || t.name().equalsIgnoreCase(v.replace(' ', '_'))) {
                return t;
            }
        }
        String lower = v.toLowerCase(Locale.ROOT);
        if (lower.startsWith("factual")) return FACTUAL_QA;
        if (lower.startsWith("analytical")) return ANALYTICAL;
        if (lower.startsWith("instruction")) return INSTRUCTION_FOLLOWING;
        if (lower.startsWith("compar")) return COMPARATIVE;
        if (lower.startsWith("cod")) return CODING;
        return null;
    }
}
