package com.example.ai_doc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS policy for the browser client.
 *
 * <p>Configured here rather than with {@code @CrossOrigin} on the controller because the
 * allowed origins differ per environment, and an annotation hard-codes the development
 * ones into the deployable.
 */
@Configuration
public class CorsConfiguration implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfiguration(
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
            String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins.clone();
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST")
                // Without these the browser cannot read the batch outcome headers: they are
                // not CORS-safelisted, so fetch() silently hides them.
                .exposedHeaders("X-Batch-Total-Count", "X-Batch-Success-Count", "X-Batch-Failed-Files");
    }
}
