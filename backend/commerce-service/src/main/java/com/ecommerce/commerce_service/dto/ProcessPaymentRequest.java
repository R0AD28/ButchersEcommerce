package com.ecommerce.commerce_service.dto;
import jakarta.validation.constraints.*;
public record ProcessPaymentRequest(
    @NotBlank @Size(max = 40) String orderNumber,
    @NotBlank @Pattern(regexp = "CARD|TRANSFER") String paymentMethod,
    @NotBlank @Size(min = 8, max = 100) String paymentToken
) {}
