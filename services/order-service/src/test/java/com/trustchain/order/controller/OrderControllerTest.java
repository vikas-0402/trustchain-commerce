package com.trustchain.order.controller;

import com.trustchain.order.domain.OrderStatus;
import com.trustchain.order.dto.CreateOrderRequest;
import com.trustchain.order.dto.OrderResponse;
import com.trustchain.order.exception.InvalidOrderException;
import com.trustchain.order.exception.OrderNotFoundException;
import com.trustchain.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final String PRODUCT = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private static String body(String customerId, String items) {
        return "{\"customerId\":\"" + customerId + "\",\"items\":" + items + "}";
    }

    private static String oneItem(String quantity, String unitPrice) {
        return "[{\"productId\":\"" + PRODUCT + "\",\"quantity\":" + quantity + ",\"unitPrice\":" + unitPrice + "}]";
    }

    private static OrderResponse sampleResponse(UUID id) {
        return new OrderResponse(id, "cust-1", OrderStatus.PENDING, new BigDecimal("899.00"),
                Instant.parse("2026-11-08T10:00:00Z"),
                List.of(new OrderResponse.Item(UUID.fromString(PRODUCT), 1, new BigDecimal("899.00"))));
    }

    private String postExpectingBadRequest(String json) throws Exception {
        String response = mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        verifyNoInteractions(orderService);
        return response;
    }

    @Test
    void createOrderReturns201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.create(any(CreateOrderRequest.class))).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(body("cust-1", oneItem("1", "899.00"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/orders/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customerId").value("cust-1"));
    }

    @Test
    void createOrderRejectsBlankCustomerId() throws Exception {
        assertThat(postExpectingBadRequest(body("", oneItem("1", "899.00")))).contains("customerId");
    }

    @Test
    void createOrderRejectsTooLongCustomerId() throws Exception {
        assertThat(postExpectingBadRequest(body("x".repeat(65), oneItem("1", "899.00")))).contains("customerId");
    }

    @Test
    void createOrderRejectsEmptyItems() throws Exception {
        assertThat(postExpectingBadRequest(body("cust-1", "[]"))).contains("items");
    }

    @Test
    void createOrderRejectsZeroQuantity() throws Exception {
        assertThat(postExpectingBadRequest(body("cust-1", oneItem("0", "899.00")))).contains("quantity");
    }

    @Test
    void createOrderRejectsNonPositiveUnitPrice() throws Exception {
        assertThat(postExpectingBadRequest(body("cust-1", oneItem("1", "0")))).contains("unitPrice");
    }

    @Test
    void createOrderRejectsMoreThanTwoDecimalPlaces() throws Exception {
        assertThat(postExpectingBadRequest(body("cust-1", oneItem("1", "10.123")))).contains("unitPrice");
    }

    @Test
    void createOrderRejectsMissingProductId() throws Exception {
        String items = "[{\"quantity\":1,\"unitPrice\":899.00}]";
        assertThat(postExpectingBadRequest(body("cust-1", items))).contains("productId");
    }

    @Test
    void createOrderMapsServiceRejectionTo400() throws Exception {
        when(orderService.create(any(CreateOrderRequest.class)))
                .thenThrow(new InvalidOrderException("Order total exceeds the maximum supported amount"));

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(body("cust-1", oneItem("1", "899.00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getOrderReturnsOrder() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.get(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void getOrderReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.get(id)).thenThrow(new OrderNotFoundException(id));

        String response = mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).contains(id.toString());
    }

    @Test
    void getOrderRejectsMalformedId() throws Exception {
        mockMvc.perform(get("/orders/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orderService);
    }

    @Test
    void listOrdersReturnsArray() throws Exception {
        when(orderService.list()).thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
