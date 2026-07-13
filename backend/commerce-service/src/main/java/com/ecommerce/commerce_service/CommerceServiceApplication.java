package com.ecommerce.commerce_service;

import com.ecommerce.commerce_service.config.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(SecurityProperties.class)
public class CommerceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommerceServiceApplication.class, args);
    }
}
