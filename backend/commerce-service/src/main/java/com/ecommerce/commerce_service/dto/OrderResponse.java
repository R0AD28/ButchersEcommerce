package com.ecommerce.commerce_service.dto;

import com.ecommerce.commerce_service.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String orderNumber,
        String customerEmail, 
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String promotionCode,
        Long version,
        Instant createdAt,
        List<OrderItemResponse> items) {
}
