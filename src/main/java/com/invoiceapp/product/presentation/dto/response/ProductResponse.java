package com.invoiceapp.product.presentation.dto.response;

import com.invoiceapp.product.domain.enums.ProductType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private ProductType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}