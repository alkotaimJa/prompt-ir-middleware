package com.jawaher.GPT.experiment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jawaher.GPT.experiment.domain.LlmResponse;
import com.jawaher.GPT.experiment.domain.Trial;
import com.jawaher.GPT.experiment.repository.TrialRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Claude returns HTTP 500 on every call: the trial must still succeed with the
 * other two providers, the failed pair must be excluded from voting, and the
 * failure must be recorded.
 */
@SpringBootTest(properties = {
        "experiment.mock-llm=true",
        "experiment.mock-fail-providers=claude",
        "spring.datasource.url=jdbc:h2:mem:fail1;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ProviderFailureIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TrialRepository trials;
    @Autowired TransactionTemplate tx;

    @Test
    void oneFailingProviderDoesNotAffectTheOthers() throws Exception {
        String pid = json.readTree(mvc.perform(post("/api/participants").contentType(MediaType.APPLICATION_JSON)
                .content("{\"consent\":true}")).andReturn().getResponse().getContentAsString()).get("participantId").asText();

        JsonNode trial = json.readTree(mvc.perform(post("/api/trials").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("participantId", pid, "prompt", "Explain how HTTP caching works"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertThat(trial.get("anyVotable").asBoolean()).isTrue();
        List<Integer> votable = new ArrayList<>();
        int failedSlot = -1;
        for (JsonNode m : trial.get("models")) {
            if (m.get("votable").asBoolean()) {
                votable.add(m.get("slot").asInt());
            } else {
                failedSlot = m.get("slot").asInt();
                assertThat(m.get("responseA").get("ok").asBoolean() && m.get("responseB").get("ok").asBoolean()).isFalse();
            }
        }
        assertThat(votable).hasSize(2);
        assertThat(failedSlot).isPositive();
        UUID trialId = UUID.fromString(trial.get("trialId").asText());

        // The failed model cannot be chosen as best.
        String badBest = """
                {"participantId":"%s","votes":[{"slot":%d,"choice":"A_BETTER","reason":"clearer"},
                 {"slot":%d,"choice":"B_BETTER","reason":"shorter"}],"bestSlot":%d}"""
                .formatted(pid, votable.get(0), votable.get(1), failedSlot);
        mvc.perform(post("/api/trials/" + trialId + "/evaluation").contentType(MediaType.APPLICATION_JSON).content(badBest))
                .andExpect(status().isBadRequest());

        // Votes for the two working providers are accepted.
        String ok = badBest.replace("\"bestSlot\":" + failedSlot, "\"bestSlot\":" + votable.get(0));
        mvc.perform(post("/api/trials/" + trialId + "/evaluation").contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isOk());

        tx.executeWithoutResult(s -> {
            Trial t = trials.findById(trialId).orElseThrow();
            assertThat(t.getVotes()).hasSize(2).noneMatch(v -> v.getProvider().equals("claude"));
            List<LlmResponse> claude = t.getResponses().stream().filter(r -> r.getProvider().equals("claude")).toList();
            assertThat(claude).hasSize(2).allMatch(r -> !r.isSuccess() && r.getErrorMessage().contains("500"));
            assertThat(t.getResponses().stream().filter(r -> !r.getProvider().equals("claude"))).allMatch(LlmResponse::isSuccess);
        });
    }
}
