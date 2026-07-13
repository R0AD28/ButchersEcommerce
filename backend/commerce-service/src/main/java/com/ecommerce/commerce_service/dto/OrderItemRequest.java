package com.ecommerce.commerce_service.dto;
import jakarta.validation.constraints.*;
public record OrderItemRequest(
    @NotBlank @Size(max = 50) String sku,
    @Min(1) @Max(100) int quantity
) {}
