package com.ecommerce.commerce_service.dto;
import com.ecommerce.commerce_service.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
public record PaymentResponse(String paymentReference, String orderNumber, BigDecimal amount,
 PaymentStatus status, String paymentMethod, Instant processedAt) {}
