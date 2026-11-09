package com.trustchain.order;

import com.trustchain.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real application against a throwaway PostgreSQL container, so Flyway migrations,
 * Hibernate schema validation and persistence are all exercised. Skipped when Docker is unavailable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class OrderApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    private static String validBody(String customerId) {
        return """
                {"customerId":"%s","items":[
                  {"productId":"11111111-1111-1111-1111-111111111111","quantity":2,"unitPrice":899.00},
                  {"productId":"22222222-2222-2222-2222-222222222222","quantity":1,"unitPrice":59.00}]}
                """.formatted(customerId);
    }

    @Test
    void createdOrderCanBeFetchedAndIsPending() throws Exception {
        String location = mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("it-" + UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getHeader("Location");

        assertThat(location).startsWith("/orders/");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalAmount").value(1857.0));
    }

    @Test
    void createdOrderAppearsInList() throws Exception {
        String customerId = "it-" + UUID.randomUUID();
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(validBody(customerId)))
                .andExpect(status().isCreated());

        String list = mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(list).contains(customerId);
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        mockMvc.perform(get("/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidOrderReturns400AndIsNotStored() throws Exception {
        long before = orderRepository.count();

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"\",\"items\":[]}"))
                .andExpect(status().isBadRequest());

        assertThat(orderRepository.count()).isEqualTo(before);
    }

    @Test
    void orderWithOverflowingTotalReturns400AndIsNotStored() throws Exception {
        long before = orderRepository.count();
        String body = """
                {"customerId":"it-overflow","items":[
                  {"productId":"11111111-1111-1111-1111-111111111111","quantity":2,"unitPrice":9999999999.99}]}
                """;

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        assertThat(orderRepository.count()).isEqualTo(before);
    }
}
