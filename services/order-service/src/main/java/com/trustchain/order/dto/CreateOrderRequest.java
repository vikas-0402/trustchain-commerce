package com.trustchain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Limits mirror the database columns: customer_id VARCHAR(64), unit_price NUMERIC(12,2).
 *
 * <p>PHASE 1 SIMPLIFICATION: {@code unitPrice} is supplied by the client and is NOT verified
 * against inventory data. This is temporary and must be replaced when the Order service
 * integrates with the Inventory service (see docs/development-roadmap.md).
 */
public record CreateOrderRequest(
        @NotBlank @Size(max = 64) String customerId,
        @NotEmpty @Valid List<Item> items) {

    public record Item(
            @NotNull UUID productId,
            @Min(1) int quantity,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice) {
    }
}
