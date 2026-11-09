package com.trustchain.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Limits mirror the database columns: sku VARCHAR(64), name VARCHAR(255), price NUMERIC(12,2). */
public record CreateProductRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Min(0) int stockQuantity) {
}
