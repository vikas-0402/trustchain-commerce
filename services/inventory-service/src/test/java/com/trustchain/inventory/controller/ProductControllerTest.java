package com.trustchain.inventory.controller;

import com.trustchain.inventory.dto.CreateProductRequest;
import com.trustchain.inventory.dto.ProductResponse;
import com.trustchain.inventory.exception.DuplicateSkuException;
import com.trustchain.inventory.exception.ProductNotFoundException;
import com.trustchain.inventory.service.ProductService;
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

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private static String body(String sku, String name, String price, String stock) {
        return "{\"sku\":\"" + sku + "\",\"name\":\"" + name + "\",\"price\":" + price
                + ",\"stockQuantity\":" + stock + "}";
    }

    private static ProductResponse sampleResponse(UUID id) {
        return new ProductResponse(id, "SKU-1", "Widget", new BigDecimal("10.00"), 5,
                Instant.parse("2026-11-08T10:00:00Z"));
    }

    private String postExpectingBadRequest(String json) throws Exception {
        String response = mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        verifyNoInteractions(productService);
        return response;
    }

    @Test
    void createProductReturns201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.create(any(CreateProductRequest.class))).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                        .content(body("SKU-1", "Widget", "10.00", "5")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/products/" + id))
                .andExpect(jsonPath("$.sku").value("SKU-1"));
    }

    @Test
    void createProductRejectsBlankSku() throws Exception {
        assertThat(postExpectingBadRequest(body("", "Widget", "10.00", "5"))).contains("sku");
    }

    @Test
    void createProductRejectsTooLongSku() throws Exception {
        assertThat(postExpectingBadRequest(body("S".repeat(65), "Widget", "10.00", "5"))).contains("sku");
    }

    @Test
    void createProductRejectsBlankName() throws Exception {
        assertThat(postExpectingBadRequest(body("SKU-1", "", "10.00", "5"))).contains("name");
    }

    @Test
    void createProductRejectsTooLongName() throws Exception {
        assertThat(postExpectingBadRequest(body("SKU-1", "N".repeat(256), "10.00", "5"))).contains("name");
    }

    @Test
    void createProductRejectsNonPositivePrice() throws Exception {
        assertThat(postExpectingBadRequest(body("SKU-1", "Widget", "0", "5"))).contains("price");
    }

    @Test
    void createProductRejectsMoreThanTwoDecimalPlaces() throws Exception {
        assertThat(postExpectingBadRequest(body("SKU-1", "Widget", "10.123", "5"))).contains("price");
    }

    @Test
    void createProductRejectsNegativeStock() throws Exception {
        assertThat(postExpectingBadRequest(body("SKU-1", "Widget", "10.00", "-1"))).contains("stockQuantity");
    }

    @Test
    void createProductReturns409ForDuplicateSku() throws Exception {
        when(productService.create(any(CreateProductRequest.class))).thenThrow(new DuplicateSkuException("SKU-1"));

        String response = mockMvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                        .content(body("SKU-1", "Widget", "10.00", "5")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).contains("SKU-1");
    }

    @Test
    void getProductReturnsProduct() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.get(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getProductReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.get(id)).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listProductsReturnsArray() throws Exception {
        when(productService.list()).thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
