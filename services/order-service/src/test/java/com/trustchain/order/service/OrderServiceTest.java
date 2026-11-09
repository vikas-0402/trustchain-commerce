package com.trustchain.order.service;

import com.trustchain.order.domain.Order;
import com.trustchain.order.domain.OrderStatus;
import com.trustchain.order.dto.CreateOrderRequest;
import com.trustchain.order.dto.OrderResponse;
import com.trustchain.order.exception.InvalidOrderException;
import com.trustchain.order.exception.OrderNotFoundException;
import com.trustchain.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final UUID LAPTOP = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID HEADPHONES = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createStoresPendingOrderWithComputedTotal() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateOrderRequest request = new CreateOrderRequest("cust-1", List.of(
                new CreateOrderRequest.Item(LAPTOP, 2, new BigDecimal("899.00")),
                new CreateOrderRequest.Item(HEADPHONES, 1, new BigDecimal("59.00"))));

        OrderResponse response = orderService.create(request);

        assertThat(response.customerId()).isEqualTo("cust-1");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.totalAmount()).isEqualByComparingTo("1857.00");
        assertThat(response.items()).hasSize(2);
    }

    @Test
    void createRejectsTotalThatExceedsColumnLimit() {
        CreateOrderRequest request = new CreateOrderRequest("cust-1", List.of(
                new CreateOrderRequest.Item(LAPTOP, 2, new BigDecimal("9999999999.99"))));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(InvalidOrderException.class);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getReturnsExistingOrder() {
        Order order = Order.create("cust-2");
        order.addItem(LAPTOP, 1, new BigDecimal("899.00"));
        when(orderRepository.findById(any(UUID.class))).thenReturn(Optional.of(order));

        OrderResponse response = orderService.get(UUID.randomUUID());

        assertThat(response.customerId()).isEqualTo("cust-2");
        assertThat(response.totalAmount()).isEqualByComparingTo("899.00");
    }

    @Test
    void getThrowsWhenOrderDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.get(id))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void listReturnsOrders() {
        Order order = Order.create("cust-3");
        order.addItem(HEADPHONES, 3, new BigDecimal("59.00"));
        when(orderRepository.findAll(any(Sort.class))).thenReturn(List.of(order));

        List<OrderResponse> result = orderService.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).totalAmount()).isEqualByComparingTo("177.00");
    }
}
