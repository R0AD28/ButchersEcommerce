package com.ecommerce.commerce_service.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record CreateOrderRequest(
    @NotEmpty @Size(max = 50) List<@Valid OrderItemRequest> items,
    @Size(max = 50) String promotionCode
) {}
