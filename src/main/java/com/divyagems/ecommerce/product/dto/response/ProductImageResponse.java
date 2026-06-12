package com.divyagems.ecommerce.product.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductImageResponse {
    private UUID id;
    private String imageUrl;
    private String altText;
    private int displayOrder;
    private boolean isPrimary;
}
