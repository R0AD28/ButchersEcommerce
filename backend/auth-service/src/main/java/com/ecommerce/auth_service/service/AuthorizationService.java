package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.config.SecurityProperties;
import com.ecommerce.auth_service.dto.AuthorizationContext;
import com.ecommerce.auth_service.exception.BusinessException;
import com.ecommerce.auth_service.exception.ErrorCode;
import com.ecommerce.auth_service.model.User;
import com.ecommerce.auth_service.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthorizationService {

    private static final String CACHE_PREFIX =
            "auth:user:";

    private static final String CACHE_SUFFIX =
            ":authorities";

    private final UserRepository userRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    private final SecurityProperties securityProperties;

    public AuthorizationService(
            UserRepository userRepository,
            RedisTemplate<String, Object> redisTemplate,
            SecurityProperties securityProperties
    ) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.securityProperties = securityProperties;
    }

    @Transactional(readOnly = true)
    public AuthorizationContext getAuthorizationContext(
            Long userId
    ) {

        String cacheKey = buildCacheKey(userId);

        Object cachedValue =
                redisTemplate
                        .opsForValue()
                        .get(cacheKey);

        if (cachedValue instanceof AuthorizationContext context) {
            return context;
        }

        User user = userRepository
                .findWithAuthoritiesById(userId)
                .orElseThrow(
                        () -> new BusinessException(
                                HttpStatus.NOT_FOUND,
                                ErrorCode.USER_NOT_FOUND,
                                "El usuario no existe"
                        )
                );

        Set<String> roles =
                user.getRole() == null
                        ? Set.of()
                        : Set.of(
                                user.getRole().getName()
                        );

        Set<String> permissions =
                user.getRole() == null
                        ? Set.of()
                        : user.getRole()
                        .getPermissions()
                        .stream()
                        .map(permission ->
                                permission.getName()
                        )
                        .collect(
                                Collectors.toUnmodifiableSet()
                        );

        AuthorizationContext context =
                new AuthorizationContext(
                        user.getId(),
                        user.getEmail(),
                        user.isEnabled(),
                        user.isTemporarilyLocked(),
                        roles,
                        permissions
                );

        redisTemplate.opsForValue().set(
                cacheKey,
                context,
                Duration.ofSeconds(
                        securityProperties
                                .authorizationCache()
                                .ttlSeconds()
                )
        );

        return context;
    }

    public void evict(Long userId) {

        if (userId == null) {
            return;
        }

        redisTemplate.delete(
                buildCacheKey(userId)
        );
    }

    private String buildCacheKey(Long userId) {

        return CACHE_PREFIX
                + userId
                + CACHE_SUFFIX;
    }
}