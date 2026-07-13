package com.ecommerce.catalog_service.dto;
import jakarta.validation.constraints.*;
public record ProductStatusRequest(
        @NotBlank 
        @Size(max=50) 
        @Pattern(regexp="^[A-Za-z0-9_-]+$") String sku,
        @NotNull 
        @Min(0) Long expectedVersion,
        @NotNull Boolean active
) {}
