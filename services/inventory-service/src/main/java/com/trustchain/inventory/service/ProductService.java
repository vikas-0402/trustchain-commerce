package com.trustchain.inventory.service;

import com.trustchain.inventory.domain.Product;
import com.trustchain.inventory.dto.CreateProductRequest;
import com.trustchain.inventory.dto.ProductResponse;
import com.trustchain.inventory.exception.DuplicateSkuException;
import com.trustchain.inventory.exception.ProductNotFoundException;
import com.trustchain.inventory.repository.ProductRepository;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    /** PostgreSQL's default name for the UNIQUE constraint on products.sku (see V1 migration). */
    static final String SKU_UNIQUE_CONSTRAINT = "products_sku_key";

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        // Fast path: friendly rejection in the common case.
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException(request.sku());
        }
        Product product = Product.create(request.sku(), request.name(), request.price(), request.stockQuantity());
        try {
            // Flush now so a concurrent insert of the same SKU surfaces here, not at commit time.
            return ProductResponse.from(productRepository.saveAndFlush(product));
        } catch (DataIntegrityViolationException ex) {
            if (isSkuUniqueViolation(ex)) {
                throw new DuplicateSkuException(request.sku());
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    private static boolean isSkuUniqueViolation(DataIntegrityViolationException ex) {
        String message = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
        return message != null && message.contains(SKU_UNIQUE_CONSTRAINT);
    }
}
