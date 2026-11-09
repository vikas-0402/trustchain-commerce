package com.trustchain.inventory.controller;

import com.trustchain.inventory.dto.CreateProductRequest;
import com.trustchain.inventory.dto.ProductResponse;
import com.trustchain.inventory.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.create(request);
        return ResponseEntity.created(URI.create("/products/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        return productService.get(id);
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productService.list();
    }
}
