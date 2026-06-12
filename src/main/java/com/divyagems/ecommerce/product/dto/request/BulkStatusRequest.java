package com.divyagems.ecommerce.product.dto.request;

import com.divyagems.ecommerce.enums.ProductStatusEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Request body for bulk product status update (admin only).
 */
@Getter
@Setter
@NoArgsConstructor
public class BulkStatusRequest {

    @NotEmpty(message = "Product IDs list must not be empty")
    private List<UUID> productIds;

    @NotNull(message = "Status is required")
    private ProductStatusEnum status;
}
