package com.ecommerce.catalog_service.dto;
import java.math.BigDecimal;
import java.time.Instant;
public record ManagementProductResponse(String sku, String name, String description, BigDecimal price,
                                        Integer stock, boolean active, Long version,
                                        Instant createdAt, Instant updatedAt) {}
