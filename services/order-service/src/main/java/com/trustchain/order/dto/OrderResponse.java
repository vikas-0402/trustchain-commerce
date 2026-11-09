package com.trustchain.order.dto;

import com.trustchain.order.domain.Order;
import com.trustchain.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        List<Item> items) {

    public record Item(UUID productId, int quantity, BigDecimal unitPrice) {
    }

    public static OrderResponse from(Order order) {
        List<Item> items = order.getItems().stream()
                .map(i -> new Item(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getStatus(),
                order.getTotalAmount(), order.getCreatedAt(), items);
    }
}
