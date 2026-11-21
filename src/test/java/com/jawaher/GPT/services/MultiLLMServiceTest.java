package com.jawaher.GPT.services;

import com.jawaher.GPT.DTO.ChatRequest;
import com.jawaher.GPT.DTO.MultiLLMResponse;
import com.jawaher.GPT.pipeline.TestPipelines;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** One provider failing (HTTP 500) must not prevent the others from answering. */
class MultiLLMServiceTest {

    private static ChatClient answering(String text) {
        ChatClient c = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(c.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(text);
        when(c.prompt().user(anyString()).call().content()).thenReturn(text);
        return c;
    }

    private static ChatClient failing() {
        ChatClient c = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(c.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new RuntimeException("500 Internal Server Error"));
        when(c.prompt().user(anyString()).call().content())
                .thenThrow(new RuntimeException("500 Internal Server Error"));
        return c;
    }

    @Test
    void failingProviderDoesNotBlockOthers() {
        MultiLLMService service = new MultiLLMService(answering("gpt ok"), failing(), answering("gemini ok"),
                Executors.newFixedThreadPool(3), TestPipelines.pipeline(""), 10);
        ChatRequest r = new ChatRequest();
        r.setMessage("Compare REST and GraphQL");

        MultiLLMResponse res = service.chat(r);

        assertThat(res.getGpt()).isEqualTo("gpt ok");
        assertThat(res.getGemini()).isEqualTo("gemini ok");
        assertThat(res.getClaude()).isEqualTo("Claude error: service temporarily unavailable");
    }
}
