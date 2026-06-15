package com.divyagems.ecommerce.search.service;

import com.divyagems.ecommerce.entity.Product;
import com.divyagems.ecommerce.entity.ProductImage;
import com.divyagems.ecommerce.enums.TagTypeEnum;
import com.divyagems.ecommerce.product.dto.request.ProductFilterRequest;
import com.divyagems.ecommerce.product.dto.response.ProductPageResponse;
import com.divyagems.ecommerce.product.repository.ProductRepository;
import com.divyagems.ecommerce.product.repository.ProductVariantRepository;
import com.divyagems.ecommerce.product.service.ProductService;
import com.divyagems.ecommerce.repository.TagRepository;
import com.divyagems.ecommerce.search.dto.response.FilterOptionsResponse;
import com.divyagems.ecommerce.search.dto.response.SearchSuggestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int SUGGESTION_LIMIT = 5;

    private final ProductRepository        productRepository;
    private final ProductVariantRepository variantRepository;
    private final TagRepository            tagRepository;
    private final ProductService           productService;

    // ─── Full-Text Search ──────────────────────────────────────

    /**
     * Full product search using ILIKE on name + description + SKU.
     * Delegates to the existing ProductService.getProducts() which uses ProductSpecification.
     */
    @Transactional(readOnly = true)
    public ProductPageResponse search(String keyword, int page, int size) {
        if (!StringUtils.hasText(keyword)) {
            return ProductPageResponse.builder()
                    .content(Collections.emptyList())
                    .page(page).size(size)
                    .totalElements(0).totalPages(0)
                    .first(true).last(true).empty(true)
                    .build();
        }

        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setKeyword(keyword.trim());
        filter.setPage(page);
        filter.setSize(size);

        return productService.getProducts(filter, false /* public mode */);
    }

    // ─── Autocomplete Suggestions ──────────────────────────────

    @Transactional(readOnly = true)
    public List<SearchSuggestion> getSuggestions(String keyword) {
        if (!StringUtils.hasText(keyword) || keyword.trim().length() < 2) {
            return Collections.emptyList();
        }

        Pageable top5 = PageRequest.of(0, SUGGESTION_LIMIT, Sort.by("averageRating").descending());
        List<Product> products = productRepository.findSuggestions(keyword.trim(), top5);

        return products.stream()
                .map(p -> {
                    String imageUrl = p.getImages().stream()
                            .filter(ProductImage::isPrimary)
                            .findFirst()
                            .map(ProductImage::getImageUrl)
                            .orElse(p.getImages().isEmpty() ? null
                                    : p.getImages().get(0).getImageUrl());

                    return SearchSuggestion.builder()
                            .productId(p.getId())
                            .name(p.getName())
                            .slug(p.getSlug())
                            .primaryImageUrl(imageUrl)
                            .price(p.getPrice())
                            .salePrice(p.getSalePrice())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─── Filter Options ────────────────────────────────────────

    @Transactional(readOnly = true)
    public FilterOptionsResponse getFilterOptions() {
        // Tag-based filters by type
        List<String> materials = tagNames(TagTypeEnum.MATERIAL);
        List<String> planets   = tagNames(TagTypeEnum.PLANET);
        List<String> chakras   = tagNames(TagTypeEnum.CHAKRA);
        List<String> elements  = tagNames(TagTypeEnum.ELEMENT);
        List<String> benefits  = tagNames(TagTypeEnum.BENEFIT);
        List<String> vastuUses = tagNames(TagTypeEnum.VASTU_USE);

        // Color variants
        List<FilterOptionsResponse.ColorOption> colors = variantRepository
                .findDistinctActiveColors()
                .stream()
                .map(row -> FilterOptionsResponse.ColorOption.builder()
                        .name((String) row[0])
                        .hexCode(row[1] != null ? (String) row[1] : null)
                        .build())
                .collect(Collectors.toList());

        // Price range
        var minPrice = productRepository.findMinActivePrice();
        var maxPrice = productRepository.findMaxActivePrice();

        return FilterOptionsResponse.builder()
                .materials(materials)
                .planets(planets)
                .chakras(chakras)
                .elements(elements)
                .benefits(benefits)
                .vastuUses(vastuUses)
                .colors(colors)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();
    }

    // ─── Private ──────────────────────────────────────────────

    private List<String> tagNames(TagTypeEnum type) {
        return tagRepository.findByTagType(type).stream()
                .map(t -> t.getName())
                .sorted()
                .collect(Collectors.toList());
    }
}
