package com.invoiceapp.product.presentation.controller;

import com.invoiceapp.common.dto.ApiResponse;
import com.invoiceapp.product.application.service.ProductService;
import com.invoiceapp.product.presentation.dto.request.ProductRequest;
import com.invoiceapp.product.presentation.dto.response.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            @RequestAttribute("userId") UUID userId
    ) {
        ProductResponse response = productService.createProduct(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", response));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest request,
            @RequestAttribute("userId") UUID userId
    ) {
        ProductResponse response = productService.updateProduct(productId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID productId,
            @RequestAttribute("userId") UUID userId
    ) {
        productService.deleteProduct(productId, userId);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable UUID productId,
            @RequestAttribute("userId") UUID userId
    ) {
        ProductResponse response = productService.getProductById(productId, userId);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> response = productService.getAllProducts(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", response));
    }
}