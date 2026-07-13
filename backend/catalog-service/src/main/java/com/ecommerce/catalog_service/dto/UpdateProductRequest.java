package com.ecommerce.catalog_service.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record UpdateProductRequest(
        @NotBlank 
        @Size(max=50) 
        @Pattern(regexp="^[A-Za-z0-9_-]+$") String sku,
        @NotNull 
        @Min(0) Long expectedVersion,
        @NotBlank 
        @Size(max=150) String name,
        @Size(max=1000) String description,
        @NotNull 
        @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal price) {}
