package com.ecommerce.catalog_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient.Builder restClientBuilder(SecurityProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.httpClient().connectTimeoutMillis()));
        factory.setReadTimeout(Duration.ofMillis(properties.httpClient().readTimeoutMillis()));
        return RestClient.builder().requestFactory(factory);
    }
}
