package com.jawaher.GPT.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Translates a Heroku/Render/Neon style {@code DATABASE_URL}
 * ({@code postgres://user:pass@host:port/db?sslmode=require}) into Spring's
 * {@code spring.datasource.*} properties. When {@code DATABASE_URL} is absent
 * the application falls back to the file-based H2 database configured in
 * application.properties.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        String url = env.getProperty("DATABASE_URL");
        if (url == null || url.isBlank()) {
            return;
        }
        Map<String, Object> props = new HashMap<>();
        if (url.startsWith("jdbc:")) {
            props.put("spring.datasource.url", url);
        } else if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            URI uri = URI.create(url.replaceFirst("^postgres(ql)?://", "http://"));
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String query = uri.getRawQuery();
            String jdbc = "jdbc:postgresql://" + host + ":" + port + uri.getRawPath()
                    + (query == null ? "" : "?" + query);
            props.put("spring.datasource.url", jdbc);
            String userInfo = uri.getRawUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                props.put("spring.datasource.username", decode(parts[0]));
                if (parts.length > 1) {
                    props.put("spring.datasource.password", decode(parts[1]));
                }
            }
        } else {
            return;
        }
        props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        env.getPropertySources().addFirst(new MapPropertySource("databaseUrl", props));
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
