package com.jawaher.GPT.experiment;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime settings for the raw-vs-transformed evaluation study
 * ({@code experiment.*} in application.properties / environment variables).
 */
@ConfigurationProperties(prefix = "experiment")
public class ExperimentProperties {

    /** When true, LLM calls are answered by a deterministic stub (no API keys or cost). */
    private boolean mockLlm = false;

    /** Optional code participants must enter to join. Blank = open study. */
    private String accessCode = "";

    /** Token required for /api/admin/** (results, CSV export). Blank = admin API disabled. */
    private String adminToken = "";

    /** Providers that take part in the study, in canonical order. */
    private List<String> providers = new ArrayList<>(List.of("openai", "claude", "gemini"));

    /** Maximum length of a participant prompt, in characters. */
    private int maxPromptChars = 4_000;

    /** Maximum number of trials a single participant may start (cost control). */
    private int maxTrialsPerParticipant = 25;

    /** Maximum number of trials that may be started from one IP address per hour. */
    private int maxTrialsPerIpPerHour = 40;

    /** Timeout for each individual LLM call, in seconds. */
    private int callTimeoutSeconds = 120;

    /** Minimum length of a participant's reason for each vote (0 = optional). */
    private int minReasonChars = 3;

    public boolean isMockLlm() { return mockLlm; }
    public void setMockLlm(boolean mockLlm) { this.mockLlm = mockLlm; }
    public String getAccessCode() { return accessCode; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
    public String getAdminToken() { return adminToken; }
    public void setAdminToken(String adminToken) { this.adminToken = adminToken; }
    public List<String> getProviders() { return providers; }
    public void setProviders(List<String> providers) { this.providers = providers; }
    public int getMaxPromptChars() { return maxPromptChars; }
    public void setMaxPromptChars(int maxPromptChars) { this.maxPromptChars = maxPromptChars; }
    public int getMaxTrialsPerParticipant() { return maxTrialsPerParticipant; }
    public void setMaxTrialsPerParticipant(int v) { this.maxTrialsPerParticipant = v; }
    public int getMaxTrialsPerIpPerHour() { return maxTrialsPerIpPerHour; }
    public void setMaxTrialsPerIpPerHour(int v) { this.maxTrialsPerIpPerHour = v; }
    public int getCallTimeoutSeconds() { return callTimeoutSeconds; }
    public void setCallTimeoutSeconds(int callTimeoutSeconds) { this.callTimeoutSeconds = callTimeoutSeconds; }
    public int getMinReasonChars() { return minReasonChars; }
    public void setMinReasonChars(int minReasonChars) { this.minReasonChars = minReasonChars; }
}
