package com.trustchain.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Product() {
    }

    public static Product create(String sku, String name, BigDecimal price, int stockQuantity) {
        Product p = new Product();
        p.sku = sku;
        p.name = name;
        p.price = price;
        p.stockQuantity = stockQuantity;
        p.createdAt = Instant.now();
        return p;
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
    public Instant getCreatedAt() { return createdAt; }
}
