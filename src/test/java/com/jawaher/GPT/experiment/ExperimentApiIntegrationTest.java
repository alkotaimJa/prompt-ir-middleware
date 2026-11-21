package com.jawaher.GPT.experiment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jawaher.GPT.experiment.domain.LlmResponse;
import com.jawaher.GPT.experiment.domain.Preference;
import com.jawaher.GPT.experiment.domain.PromptCondition;
import com.jawaher.GPT.experiment.domain.Trial;
import com.jawaher.GPT.experiment.domain.Vote;
import com.jawaher.GPT.experiment.repository.TrialRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end test of the experiment flow (register → trial → evaluation →
 * admin stats/export) using the mock LLM gateway and an in-memory database.
 */
@SpringBootTest(properties = {
        "experiment.mock-llm=true",
        "experiment.admin-token=test-admin",
        "experiment.max-trials-per-participant=3",
        "spring.datasource.url=jdbc:h2:mem:exp;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ExperimentApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TrialRepository trials;
    @Autowired TransactionTemplate tx;

    private String register() throws Exception {
        String body = mvc.perform(post("/api/participants").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consent\":true,\"background\":\"Student\",\"llmFamiliarity\":3}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("participantId").asText();
    }

    private JsonNode startTrial(String pid, String prompt) throws Exception {
        String body = mvc.perform(post("/api/trials").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of(
                                "participantId", pid, "taskCategory", "Factual QA", "prompt", prompt))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    @Test
    void fullFlow_storesRawAndTransformedPromptsResponsesVotesAndReasons() throws Exception {
        String pid = register();
        JsonNode trial = startTrial(pid, "What is the difference between REST and SOAP?");

        // Blinded view: 3 models, each with A and B, no provider or condition leaked.
        assertThat(trial.get("models")).hasSize(3);
        String view = trial.toString();
        assertThat(view).doesNotContain("openai", "claude", "gemini", "RAW", "TRANSFORMED");
        for (JsonNode m : trial.get("models")) {
            assertThat(m.get("votable").asBoolean()).isTrue();
            assertThat(m.get("responseA").get("label").asText()).isEqualTo("A");
            assertThat(m.get("responseB").get("label").asText()).isEqualTo("B");
        }

        UUID trialId = UUID.fromString(trial.get("trialId").asText());

        // Vote "A better" for every model, with a reason.
        String eval = """
                {"participantId":"%s",
                 "votes":[{"slot":1,"choice":"A_BETTER","reason":"clearer"},
                          {"slot":2,"choice":"A_BETTER","reason":"more complete"},
                          {"slot":3,"choice":"BOTH_GOOD","reason":"same quality"}],
                 "bestSlot":2, "bestReason":"most detailed"}""".formatted(pid);
        mvc.perform(post("/api/trials/" + trialId + "/evaluation").contentType(MediaType.APPLICATION_JSON).content(eval))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votesRecorded").value(3));

        // Persisted data: raw + transformed prompt, 6 responses, 3 votes resolved correctly.
        tx.executeWithoutResult(s -> {
            Trial t = trials.findById(trialId).orElseThrow();
            assertThat(t.getRawPrompt()).isEqualTo("What is the difference between REST and SOAP?");
            assertThat(t.getPromptIrJson()).contains("taskInstruction");
            assertThat(t.getTransformedPromptsJson()).contains("systemText");
            assertThat(t.getResponses()).hasSize(6);
            assertThat(t.getVotes()).hasSize(3);
            assertThat(t.getBestProvider()).isIn("openai", "claude", "gemini");
            assertThat(t.getBestProviderReason()).isEqualTo("most detailed");

            for (LlmResponse r : t.getResponses()) {
                assertThat(r.isSuccess()).isTrue();
                if (r.getCondition() == PromptCondition.RAW) {
                    assertThat(r.getSystemPrompt()).isNull();
                    assertThat(r.getUserPrompt()).isEqualTo(t.getRawPrompt());
                } else {
                    assertThat(r.getSystemPrompt()).startsWith("You are");
                    assertThat(r.getUserPrompt()).contains("Your task is:");
                }
            }
            String[] order = t.getProviderDisplayOrder().split(",");
            for (Vote v : t.getVotes()) {
                LlmResponse raw = t.getResponses().stream()
                        .filter(r -> r.getProvider().equals(v.getProvider()) && r.getCondition() == PromptCondition.RAW)
                        .findFirst().orElseThrow();
                if (v.getProvider().equals(order[2])) {
                    assertThat(v.getPreference()).isEqualTo(Preference.TIE);
                } else {
                    Preference expected = raw.getDisplayLabel().equals("A") ? Preference.RAW : Preference.TRANSFORMED;
                    assertThat(v.getPreference()).isEqualTo(expected);
                }
                assertThat(v.getReason()).isNotBlank();
            }
        });

        // Cannot evaluate twice.
        mvc.perform(post("/api/trials/" + trialId + "/evaluation").contentType(MediaType.APPLICATION_JSON).content(eval))
                .andExpect(status().isConflict());

        // Admin stats + CSV.
        mvc.perform(get("/api/admin/stats").header("X-Admin-Token", "test-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overall.total").value(3))
                .andExpect(jsonPath("$.overall.tie").value(1));
        String csv = mvc.perform(get("/api/admin/export/votes.csv").header("X-Admin-Token", "test-admin"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(csv).contains("raw_prompt").contains("REST and SOAP");
    }

    @Test
    void evaluation_requiresAReasonForEachVote() throws Exception {
        String pid = register();
        JsonNode trial = startTrial(pid, "Explain how HTTP status codes work");
        String eval = """
                {"participantId":"%s",
                 "votes":[{"slot":1,"choice":"A_BETTER","reason":""},
                          {"slot":2,"choice":"B_BETTER","reason":"ok"},
                          {"slot":3,"choice":"BOTH_BAD","reason":"both wrong"}],
                 "bestSlot":0}""".formatted(pid);
        mvc.perform(post("/api/trials/" + trial.get("trialId").asText() + "/evaluation")
                        .contentType(MediaType.APPLICATION_JSON).content(eval))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registration_requiresConsent() throws Exception {
        mvc.perform(post("/api/participants").contentType(MediaType.APPLICATION_JSON).content("{\"consent\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void trials_areCappedPerParticipant() throws Exception {
        String pid = register();
        for (int i = 0; i < 3; i++) {
            startTrial(pid, "Question number " + i);
        }
        mvc.perform(post("/api/trials").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantId\":\"" + pid + "\",\"prompt\":\"one more\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void admin_rejectsWrongToken() throws Exception {
        mvc.perform(get("/api/admin/stats").header("X-Admin-Token", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void frontendIsServed() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk());
        mvc.perform(get("/admin.html")).andExpect(status().isOk());
    }
}
