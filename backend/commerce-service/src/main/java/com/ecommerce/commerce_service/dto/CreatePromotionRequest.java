package com.ecommerce.commerce_service.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
public record CreatePromotionRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,50}") String code,
    @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal discountPercent,
    @NotNull Instant validFrom,
    @NotNull Instant validUntil,
    boolean active
) {}
