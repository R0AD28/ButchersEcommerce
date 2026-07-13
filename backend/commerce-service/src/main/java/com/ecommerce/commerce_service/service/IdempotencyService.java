package com.ecommerce.commerce_service.service;

import com.ecommerce.commerce_service.config.SecurityProperties;
import com.ecommerce.commerce_service.exception.BusinessException;
import com.ecommerce.commerce_service.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class IdempotencyService {
    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties properties;

    public IdempotencyService(StringRedisTemplate redisTemplate, SecurityProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void reserve(String key, Long userId) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_IDEMPOTENCY_KEY,
                    "Debe enviar un Idempotency-Key válido");
        }
        String redisKey = "idempotency:commerce:" + userId + ":" + key.trim();
        Boolean created = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING",
                Duration.ofHours(properties.idempotency().ttlHours()));
        if (!Boolean.TRUE.equals(created)) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_REQUEST,
                    "La solicitud ya fue procesada o está en ejecución");
        }
    }

    public void release(String key, Long userId) {
        if (key != null)
            redisTemplate.delete("idempotency:commerce:" + userId + ":" + key.trim());
    }

    public void complete(String key, Long userId) {
        redisTemplate.opsForValue().set("idempotency:commerce:" + userId + ":" + key.trim(), "COMPLETED",
                Duration.ofHours(properties.idempotency().ttlHours()));
    }
}
