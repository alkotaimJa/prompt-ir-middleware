package com.jawaher.GPT;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.DTO.MultiLLMResponse;
import com.jawaher.GPT.controllers.MultiLLMController;
import com.jawaher.GPT.services.MultiLLMService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MultiLLMController.class)
class MultiLLMControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MultiLLMService multiLLMService;

    @Test
    void multiChat_returnsResponsesFromAllThreeModels() throws Exception {
        MultiLLMResponse mockResponse = new MultiLLMResponse("GPT answer", "Claude answer", "Gemini answer");
        when(multiLLMService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/chat/multi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"What is AI?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gpt").value("GPT answer"))
                .andExpect(jsonPath("$.claude").value("Claude answer"))
                .andExpect(jsonPath("$.gemini").value("Gemini answer"));
    }

    @Test
    void multiChat_serviceThrowsException_returns500() throws Exception {
        when(multiLLMService.chat(any(ChatRequest.class))).thenThrow(new RuntimeException("LLM unavailable"));

        mockMvc.perform(post("/api/chat/multi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"fail\"}"))
                .andExpect(status().isInternalServerError());
    }
}
