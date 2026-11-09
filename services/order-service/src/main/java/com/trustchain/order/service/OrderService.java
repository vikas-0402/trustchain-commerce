package com.trustchain.order.service;

import com.trustchain.order.domain.Order;
import com.trustchain.order.dto.CreateOrderRequest;
import com.trustchain.order.dto.OrderResponse;
import com.trustchain.order.exception.InvalidOrderException;
import com.trustchain.order.exception.OrderNotFoundException;
import com.trustchain.order.repository.OrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    /** Largest value that fits orders.total_amount NUMERIC(12,2). */
    static final BigDecimal MAX_TOTAL_AMOUNT = new BigDecimal("9999999999.99");

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * PHASE 1: prices come from the client and no stock is reserved. The order is stored as PENDING.
     */
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Order order = Order.create(request.customerId());
        request.items().forEach(i -> order.addItem(i.productId(), i.quantity(), i.unitPrice()));
        if (order.getTotalAmount().compareTo(MAX_TOTAL_AMOUNT) > 0) {
            throw new InvalidOrderException("Order total exceeds the maximum supported amount");
        }
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(OrderResponse::from)
                .toList();
    }
}
