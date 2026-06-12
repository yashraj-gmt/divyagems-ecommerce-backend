package com.divyagems.ecommerce.product.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Paginated product listing response.
 * Wraps the content list with pagination metadata.
 */
@Getter
@Builder
public class ProductPageResponse {

    private List<ProductSummaryResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;
}
