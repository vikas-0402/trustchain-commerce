package com.trustchain.inventory;

import com.trustchain.inventory.domain.Product;
import com.trustchain.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real application against a throwaway PostgreSQL container, so Flyway migrations (including
 * the seed data), Hibernate schema validation and the real unique constraint are exercised.
 * Skipped when Docker is unavailable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ProductApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    private static String body(String sku) {
        return "{\"sku\":\"" + sku + "\",\"name\":\"Test product\",\"price\":12.50,\"stockQuantity\":7}";
    }

    @Test
    void seededProductsAreListed() throws Exception {
        String list = mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(list).contains("SKU-LAPTOP-01", "SKU-HEADPHONE-01", "SKU-KEYBOARD-01");
    }

    @Test
    void createdProductCanBeFetched() throws Exception {
        String location = mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                        .content(body("IT-" + UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        assertThat(location).startsWith("/products/");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(7));
    }

    @Test
    void duplicateSkuReturns409() throws Exception {
        String sku = "IT-" + UUID.randomUUID();
        mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(body(sku)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(body(sku)))
                .andExpect(status().isConflict());
    }

    @Test
    void databaseUniqueConstraintHasTheNameTheServiceRelaysOn() {
        // Bypasses the service's existsBySku check to hit the real constraint on the seeded SKU.
        assertThatThrownBy(() -> productRepository.saveAndFlush(
                Product.create("SKU-LAPTOP-01", "Duplicate", BigDecimal.ONE, 1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("products_sku_key");
    }

    @Test
    void unknownProductReturns404() throws Exception {
        mockMvc.perform(get("/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidProductReturns400() throws Exception {
        mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"\",\"name\":\"\",\"price\":0,\"stockQuantity\":-1}"))
                .andExpect(status().isBadRequest());
    }
}
