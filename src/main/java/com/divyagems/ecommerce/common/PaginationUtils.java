package com.divyagems.ecommerce.common;

import com.divyagems.ecommerce.enums.SortByEnum;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility to build Pageable from raw request parameters, with SortByEnum mapping.
 */
public final class PaginationUtils {

    private PaginationUtils() {}

    /**
     * Build a {@link Pageable} from page index, size, and sort direction.
     *
     * @param page       0-based page index (clamped to 0 if negative)
     * @param size       page size (clamped to 1–100)
     * @param sortBy     raw field name for dynamic sorting (used when sortByEnum is null)
     * @param sortByEnum typed sort enum — takes priority over sortBy
     */
    public static Pageable toPageable(int page, int size, String sortBy, SortByEnum sortByEnum) {
        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(100, size));

        Sort sort = Sort.unsorted();
        if (sortByEnum != null) {
            sort = mapSortByEnum(sortByEnum);
        } else if (sortBy != null && !sortBy.isBlank()) {
            sort = Sort.by(Sort.Direction.ASC, sortBy);
        }

        return PageRequest.of(validPage, validSize, sort);
    }

    private static Sort mapSortByEnum(SortByEnum sortByEnum) {
        return switch (sortByEnum) {
            case PRICE_ASC  -> Sort.by(Sort.Direction.ASC,  "price");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price");
            case NEWEST     -> Sort.by(Sort.Direction.DESC, "createdAt");
            case RATING     -> Sort.by(Sort.Direction.DESC, "averageRating");
            case NAME_ASC   -> Sort.by(Sort.Direction.ASC,  "name");
        };
    }
}
