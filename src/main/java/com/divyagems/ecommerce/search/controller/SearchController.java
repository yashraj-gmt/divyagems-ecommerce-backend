package com.divyagems.ecommerce.search.controller;

import com.divyagems.ecommerce.product.dto.response.ProductPageResponse;
import com.divyagems.ecommerce.search.dto.response.FilterOptionsResponse;
import com.divyagems.ecommerce.search.dto.response.SearchSuggestion;
import com.divyagems.ecommerce.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Product search, autocomplete, and filter-options endpoints.
 * All three are public — no authentication required.
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Full-text product search, autocomplete suggestions, and dynamic filter options")
public class SearchController {

    private final SearchService searchService;

    // ─── GET /search?q=...&page=0&size=20 ─────────────────────
    @Operation(
            summary = "Full-text product search",
            description = "ILIKE search across product name, description, and SKU. Returns paginated results."
    )
    @GetMapping
    public ResponseEntity<ProductPageResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.search(q, page, size));
    }

    // ─── GET /search/suggestions?q=... ───────────────────────
    @Operation(
            summary = "Autocomplete suggestions",
            description = "Returns top 5 active product names matching the keyword (min 2 chars)."
    )
    @GetMapping("/suggestions")
    public ResponseEntity<List<SearchSuggestion>> getSuggestions(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(searchService.getSuggestions(q));
    }

    // ─── GET /search/filters ──────────────────────────────────
    @Operation(
            summary = "Available filter options",
            description = "Returns all filter dimensions: tag names by type, active colors, and price range."
    )
    @GetMapping("/filters")
    public ResponseEntity<FilterOptionsResponse> getFilterOptions() {
        return ResponseEntity.ok(searchService.getFilterOptions());
    }
}
