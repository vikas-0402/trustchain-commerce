package com.trustchain.inventory.service;

import com.trustchain.inventory.domain.Product;
import com.trustchain.inventory.dto.CreateProductRequest;
import com.trustchain.inventory.dto.ProductResponse;
import com.trustchain.inventory.exception.DuplicateSkuException;
import com.trustchain.inventory.exception.ProductNotFoundException;
import com.trustchain.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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
class ProductServiceTest {

    private static final CreateProductRequest REQUEST =
            new CreateProductRequest("SKU-1", "Widget", new BigDecimal("10.00"), 5);

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createStoresProduct() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.create(REQUEST);

        assertThat(response.sku()).isEqualTo("SKU-1");
        assertThat(response.name()).isEqualTo("Widget");
        assertThat(response.price()).isEqualByComparingTo("10.00");
        assertThat(response.stockQuantity()).isEqualTo(5);
    }

    @Test
    void createRejectsKnownDuplicateSkuWithoutSaving() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(REQUEST))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("SKU-1");
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void createTranslatesConcurrentUniqueViolationToDuplicateSku() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenThrow(new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("ERROR: duplicate key value violates unique constraint \"products_sku_key\"")));

        assertThatThrownBy(() -> productService.create(REQUEST))
                .isInstanceOf(DuplicateSkuException.class);
    }

    @Test
    void createDoesNotMislabelOtherIntegrityViolationsAsDuplicateSku() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenThrow(new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("ERROR: new row violates check constraint \"products_stock_quantity_check\"")));

        assertThatThrownBy(() -> productService.create(REQUEST))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void getReturnsExistingProduct() {
        Product product = Product.create("SKU-2", "Gadget", new BigDecimal("25.00"), 3);
        when(productRepository.findById(any(UUID.class))).thenReturn(Optional.of(product));

        assertThat(productService.get(UUID.randomUUID()).sku()).isEqualTo("SKU-2");
    }

    @Test
    void getThrowsWhenProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.get(id))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void listReturnsAllProducts() {
        when(productRepository.findAll()).thenReturn(List.of(
                Product.create("A", "Alpha", BigDecimal.ONE, 1),
                Product.create("B", "Beta", BigDecimal.TEN, 2)));

        assertThat(productService.list()).hasSize(2);
    }
}
