package com.ecommerce.commerce_service.dto;
import java.math.BigDecimal;
import java.time.Instant;
public record PromotionResponse(String code, BigDecimal discountPercent, Instant validFrom, Instant validUntil, boolean active) {}
