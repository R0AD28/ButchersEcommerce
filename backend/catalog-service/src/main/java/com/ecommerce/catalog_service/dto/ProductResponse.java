package com.ecommerce.catalog_service.dto;
import java.math.BigDecimal;
public record ProductResponse(
        String sku, 
        String name, 
        String description, 
        BigDecimal price, 
        boolean available
){      }
