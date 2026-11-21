package com.jawaher.GPT.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void convertsRenderStyleUrl() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("DATABASE_URL", "postgresql://alice:p%40ss@db.internal/experiment?sslmode=require");
        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(env, null);
        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db.internal:5432/experiment?sslmode=require");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("alice");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("p@ss");
    }

    @Test
    void ignoresMissingUrl() {
        MockEnvironment env = new MockEnvironment();
        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(env, null);
        assertThat(env.getProperty("spring.datasource.url")).isNull();
    }
}
