package com.divyagems.ecommerce.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for creating or updating a product image.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductImageRequest {

    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    private String altText;

    private int displayOrder = 0;

    private boolean isPrimary = false;
}
