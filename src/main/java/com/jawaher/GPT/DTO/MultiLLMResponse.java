package com.jawaher.GPT.DTO;

public class MultiLLMResponse {

    private String gpt;
    private String claude;
    private String gemini;

    public MultiLLMResponse(String gpt, String claude, String gemini) {
        this.gpt = gpt;
        this.claude = claude;
        this.gemini = gemini;
    }

    public String getGpt() {
        return gpt;
    }

    public void setGpt(String gpt) {
        this.gpt = gpt;
    }

    public String getClaude() {
        return claude;
    }

    public void setClaude(String claude) {
        this.claude = claude;
    }

    public String getGemini() {
        return gemini;
    }

    public void setGemini(String gemini) {
        this.gemini = gemini;
    }
}
