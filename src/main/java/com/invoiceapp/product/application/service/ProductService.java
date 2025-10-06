package com.invoiceapp.product.application.service;

import com.invoiceapp.product.domain.enums.ProductType;
import com.invoiceapp.product.presentation.dto.request.ProductRequest;
import com.invoiceapp.product.presentation.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request, UUID userId);
    ProductResponse updateProduct(UUID productId, ProductRequest request, UUID userId);
    void deleteProduct(UUID productId, UUID userId);
    ProductResponse getProductById(UUID productId, UUID userId);
    Page<ProductResponse> getAllProducts(UUID userId, int page, int size, String sortBy, String sortDir, String search, ProductType type);

}