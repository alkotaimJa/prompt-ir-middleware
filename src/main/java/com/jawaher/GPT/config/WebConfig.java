package com.jawaher.GPT.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Friendly URLs for the static pages: {@code /admin} and {@code /admin/}
 * redirect to the researcher dashboard ({@code /admin.html}). Without this,
 * {@code /admin} returned a JSON "Not found" error.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/admin", "/admin.html");
        registry.addRedirectViewController("/admin/", "/admin.html");
    }
}
