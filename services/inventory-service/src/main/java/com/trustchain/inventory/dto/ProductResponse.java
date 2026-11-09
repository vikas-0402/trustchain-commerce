package com.trustchain.inventory.dto;

import com.trustchain.inventory.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        BigDecimal price,
        int stockQuantity,
        Instant createdAt) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getPrice(),
                p.getStockQuantity(), p.getCreatedAt());
    }
}
