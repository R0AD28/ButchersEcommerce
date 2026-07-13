package com.ecommerce.commerce_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record InvoiceResponse(
        String invoiceNumber,
        String orderNumber,
        String customerEmail,
        BigDecimal subtotal,
        BigDecimal discount, 
        BigDecimal total,
        Instant issuedAt
    ) {
}
