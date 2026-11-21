package com.jawaher.GPT.experiment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jawaher.GPT.experiment.ExperimentProperties;
import com.jawaher.GPT.experiment.llm.LlmGateway;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContextOptionsServiceTest {

    /** Gateway whose behaviour per provider is scripted: null = throw (simulated HTTP 500). */
    private static LlmGateway scripted(Map<String, String> responses, List<String> calls) {
        return new LlmGateway() {
            @Override
            public String complete(String provider, String systemPrompt, String userPrompt) {
                calls.add(provider);
                String r = responses.get(provider);
                if (r == null) {
                    throw new IllegalStateException("HTTP 500");
                }
                return r;
            }

            @Override
            public String modelFor(String provider) {
                return provider;
            }
        };
    }

    private static ContextOptionsService service(LlmGateway gw) {
        ExperimentProperties props = new ExperimentProperties();
        props.setContextGeneratorProviders(List.of("gemini", "openai", "claude"));
        return new ContextOptionsService(gw, props, Runnable::run, new ObjectMapper());
    }

    @Test
    void usesFirstProviderThatSucceeds() {
        List<String> calls = new ArrayList<>();
        Map<String, String> responses = new java.util.HashMap<>();
        responses.put("openai", "Sure! ```json\n[\"I am a beginner.\", \"I need code examples.\", \"I am choosing a tool.\"]\n```");
        ContextOptionsService s = service(scripted(responses, calls));

        var offer = s.offer(UUID.randomUUID(), "Compare REST and GraphQL", "Comparative reasoning");

        assertThat(calls).containsExactly("gemini", "openai");
        assertThat(offer.getValue().generator()).isEqualTo("openai");
        assertThat(offer.getValue().options()).containsExactly("I am a beginner.", "I need code examples.", "I am choosing a tool.");
    }

    @Test
    void fallsBackToRuleBasedOptionsWhenAllProvidersFail() {
        ContextOptionsService s = service(scripted(Map.of(), new ArrayList<>()));

        var offer = s.offer(UUID.randomUUID(), "q", null);

        assertThat(offer.getValue().generator()).isEqualTo(ContextOptionsService.RULE_BASED);
        assertThat(offer.getValue().options()).isEqualTo(ContextOptionsService.FALLBACK_OPTIONS);
    }

    @Test
    void offersAreSingleUse() {
        ContextOptionsService s = service(scripted(Map.of(), new ArrayList<>()));
        String token = s.offer(UUID.randomUUID(), "q", null).getKey();

        assertThat(s.take(token)).isPresent();
        assertThat(s.take(token)).isEmpty();
        assertThat(s.take("unknown")).isEmpty();
    }

    @Test
    void parseIsTolerantAndBounded() {
        ContextOptionsService s = service(scripted(Map.of(), new ArrayList<>()));
        assertThat(s.parse("no json here")).isEmpty();
        assertThat(s.parse("[\"a\", \"a\", 3, \"b\", \"c\", \"d\", \"e\"]")).containsExactly("a", "b", "c", "d");
        assertThat(s.parse("[\"" + "x".repeat(400) + "\", \"ok\"]")).containsExactly("ok");
    }
}
