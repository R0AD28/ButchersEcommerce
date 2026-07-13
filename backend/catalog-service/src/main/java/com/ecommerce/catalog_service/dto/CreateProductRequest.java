package com.ecommerce.catalog_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank(message = "El SKU es obligatorio")
        @Size(max = 50, message = "El SKU no puede superar 50 caracteres")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]+$",
                message = "El SKU solo puede contener letras, números, guiones y guiones bajos"
        )
        String sku,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,

        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        String description,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", inclusive = true, message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio debe tener máximo 10 enteros y 2 decimales")
        BigDecimal price,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock
) {
}
