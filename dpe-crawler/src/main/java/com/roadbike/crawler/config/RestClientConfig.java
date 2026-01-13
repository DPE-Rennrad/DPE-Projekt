package main.java.com.roadbike.crawler.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for HTTP client
 * Pallmann Florian
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
