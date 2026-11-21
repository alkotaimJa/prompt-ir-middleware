package com.jawaher.GPT.experiment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every provider fails: the API still answers, nothing is votable, and the participant's quota is not used. */
@SpringBootTest(properties = {
        "experiment.mock-llm=true",
        "experiment.mock-fail-providers=openai,claude,gemini",
        "spring.datasource.url=jdbc:h2:mem:fail3;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AllProvidersFailIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void allFailingProvidersAreHandledGracefully() throws Exception {
        String pid = json.readTree(mvc.perform(post("/api/participants").contentType(MediaType.APPLICATION_JSON)
                .content("{\"consent\":true}")).andReturn().getResponse().getContentAsString()).get("participantId").asText();

        // Context options fall back to the rule-based list.
        mvc.perform(post("/api/trials/context-options").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("participantId", pid, "prompt", "What is Kafka?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options.length()").value(4));

        JsonNode trial = json.readTree(mvc.perform(post("/api/trials").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("participantId", pid, "prompt", "What is Kafka?"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(trial.get("anyVotable").asBoolean()).isFalse();

        mvc.perform(post("/api/trials/" + trial.get("trialId").asText() + "/evaluation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantId\":\"" + pid + "\",\"votes\":[],\"bestSlot\":0}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/participants/" + pid)).andExpect(jsonPath("$.trialsStarted").value(0));
    }
}
