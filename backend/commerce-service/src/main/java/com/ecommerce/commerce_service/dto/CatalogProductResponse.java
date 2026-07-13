package com.ecommerce.commerce_service.dto;

import java.math.BigDecimal;

public record CatalogProductResponse(
        String sku,
        String name,
        String description, 
        BigDecimal price, 
        boolean available
    ) {
}
