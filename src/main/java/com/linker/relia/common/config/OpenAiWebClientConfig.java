package com.linker.relia.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiWebClientConfig {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    @Bean
    public WebClient openAiWebClient(@Value("${spring.ai.openai.api-key}") String apiKey) {
        return WebClient.builder()
                .baseUrl(API_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
