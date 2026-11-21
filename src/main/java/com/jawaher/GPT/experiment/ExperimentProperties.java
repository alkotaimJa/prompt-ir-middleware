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

    /** Whether participants are offered clarifying context options before the answers are generated. */
    private boolean contextStepEnabled = true;

    /** Providers tried, in order, to generate context options (falls back to rule-based options). */
    private List<String> contextGeneratorProviders = new ArrayList<>(List.of("gemini", "openai", "claude"));

    /** Timeout for each context-generation call, in seconds. */
    private int contextTimeoutSeconds = 20;

    /** Mock mode only: providers whose calls fail with a simulated HTTP 500 (for demos and tests). */
    private List<String> mockFailProviders = new ArrayList<>();

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
    public boolean isContextStepEnabled() { return contextStepEnabled; }
    public void setContextStepEnabled(boolean contextStepEnabled) { this.contextStepEnabled = contextStepEnabled; }
    public List<String> getContextGeneratorProviders() { return contextGeneratorProviders; }
    public void setContextGeneratorProviders(List<String> v) { this.contextGeneratorProviders = v; }
    public int getContextTimeoutSeconds() { return contextTimeoutSeconds; }
    public void setContextTimeoutSeconds(int contextTimeoutSeconds) { this.contextTimeoutSeconds = contextTimeoutSeconds; }
    public List<String> getMockFailProviders() { return mockFailProviders; }
    public void setMockFailProviders(List<String> mockFailProviders) { this.mockFailProviders = mockFailProviders; }
}
