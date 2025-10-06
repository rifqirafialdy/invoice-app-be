package com.invoiceapp.product.application.implement;

import com.invoiceapp.auth.domain.entity.User;
import com.invoiceapp.auth.infrastructure.repositories.UserRepository;
import com.invoiceapp.common.exception.ResourceNotFoundException;
import com.invoiceapp.common.specification.BaseSpecification;
import com.invoiceapp.product.application.service.ProductService;
import com.invoiceapp.product.domain.entity.Product;
import com.invoiceapp.product.domain.enums.ProductType;
import com.invoiceapp.product.infrastructure.repository.ProductRepository;
import com.invoiceapp.product.presentation.dto.request.ProductRequest;
import com.invoiceapp.product.presentation.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public ProductResponse createProduct(ProductRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = Product.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .type(request.getType())
                .build();

        product = productRepository.save(product);
        return mapToResponse(product);
    }

    @Override
    public ProductResponse updateProduct(UUID productId, ProductRequest request, UUID userId) {
        Product product = productRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setType(request.getType());

        product = productRepository.save(product);
        return mapToResponse(product);
    }

    @Override
    public void deleteProduct(UUID productId, UUID userId) {
        if (!productRepository.existsByIdAndUserId(productId, userId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        productRepository.deleteById(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID productId, UUID userId) {
        Product product = productRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapToResponse(product);
    }

    @Override
    public Page<ProductResponse> getAllProducts(UUID userId, int page, int size, String sortBy, String sortDir, String search, ProductType type) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Product> spec = Specification.allOf(
                BaseSpecification.<Product>withUserId(userId, "user"),
                BaseSpecification.<Product>withSearch(search, "name", "description"),
                BaseSpecification.<Product, ProductType>withEquals("type", type)
        );

        Page<Product> products = productRepository.findAll(spec, pageable);
        return products.map(this::mapToResponse);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .type(product.getType())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}