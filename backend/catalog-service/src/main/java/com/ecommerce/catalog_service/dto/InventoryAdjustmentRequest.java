package com.ecommerce.catalog_service.dto;
import jakarta.validation.constraints.*;
public record InventoryAdjustmentRequest(
 @NotBlank @Size(max=50) @Pattern(regexp="^[A-Za-z0-9_-]+$") String sku,
 @NotNull @Min(0) Long expectedVersion,
 @NotNull InventoryOperation operation,
 @NotNull @Min(0) Integer quantity,
 @NotBlank @Size(max=300) String reason) {
 public enum InventoryOperation { INCREASE, DECREASE, SET }
}
