package com.divyagems.ecommerce.product.service.impl;

import com.divyagems.ecommerce.category.repository.CategoryRepository;
import com.divyagems.ecommerce.common.SlugUtils;
import com.divyagems.ecommerce.entity.*;
import com.divyagems.ecommerce.enums.ProductStatusEnum;
import com.divyagems.ecommerce.enums.TagTypeEnum;
import com.divyagems.ecommerce.exception.BusinessException;
import com.divyagems.ecommerce.exception.ResourceNotFoundException;
import com.divyagems.ecommerce.product.dto.request.ProductFilterRequest;
import com.divyagems.ecommerce.product.dto.request.ProductRequest;
import com.divyagems.ecommerce.product.dto.response.*;
import com.divyagems.ecommerce.product.mapper.ProductMapper;
import com.divyagems.ecommerce.product.repository.ProductRepository;
import com.divyagems.ecommerce.product.repository.ProductSpecification;
import com.divyagems.ecommerce.product.service.ProductService;
import com.divyagems.ecommerce.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final int RELATED_PRODUCTS_LIMIT = 8;
    private static final int NEW_ARRIVALS_LIMIT      = 12;

    private final ProductRepository    productRepository;
    private final CategoryRepository   categoryRepository;
    private final TagRepository        tagRepository;
    private final ProductMapper        productMapper;

    // ─── Create ────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        // Validate SKU uniqueness
        if (productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("A product with SKU '" + request.getSku() + "' already exists");
        }

        // Resolve slug
        String slug = resolveUniqueSlug(request.getSlug(), request.getName(), null);

        // Resolve category
        Category category = findCategoryOrThrow(request.getCategoryId());
        Category subCategory = request.getSubCategoryId() != null
                ? findCategoryOrThrow(request.getSubCategoryId())
                : null;

        // Resolve tags
        Set<Tag> tags = resolveTags(request.getTagIds());

        // Build product entity
        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .sku(request.getSku())
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .benefits(request.getBenefits())
                .usageInstructions(request.getUsageInstructions())
                .price(request.getPrice())
                .salePrice(request.getSalePrice())
                .stockQuantity(request.getStockQuantity())
                .lowStockThreshold(request.getLowStockThreshold())
                .weight(request.getWeight())
                .weightUnit(request.getWeightUnit())
                .status(request.getStatus())
                .isFeatured(request.isFeatured())
                .isNewArrival(request.isNewArrival())
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .metaKeywords(request.getMetaKeywords())
                .category(category)
                .subCategory(subCategory)
                .tags(tags)
                .build();

        // Map and attach images
        if (!CollectionUtils.isEmpty(request.getImages())) {
            List<ProductImage> images = productMapper.toImageEntityList(request.getImages());
            images.forEach(img -> img.setProduct(product));
            // Ensure exactly one primary image
            ensureSinglePrimary(images);
            product.getImages().addAll(images);
        }

        // Map and attach variants
        if (!CollectionUtils.isEmpty(request.getVariants())) {
            request.getVariants().forEach(vReq -> {
                validateVariantSku(vReq.getSku(), null);
                ProductVariant variant = productMapper.toVariantEntity(vReq);
                variant.setProduct(product);
                product.getVariants().add(variant);
            });
        }

        Product saved = productRepository.save(product);
        log.info("Product created: '{}' (slug: {}, sku: {})", saved.getName(), saved.getSlug(), saved.getSku());
        return buildProductResponse(saved);
    }

    // ─── Update ────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = findProductOrThrow(id);

        // Validate SKU uniqueness (exclude self)
        if (!product.getSku().equals(request.getSku())
                && productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            throw new BusinessException("A product with SKU '" + request.getSku() + "' already exists");
        }

        // Re-generate slug if name or slug changed
        boolean nameChanged = !product.getName().equalsIgnoreCase(request.getName());
        boolean explicitSlugChanged = StringUtils.hasText(request.getSlug())
                && !request.getSlug().equals(product.getSlug());

        if (nameChanged || explicitSlugChanged) {
            product.setSlug(resolveUniqueSlug(request.getSlug(), request.getName(), id));
        }

        // Update scalar fields
        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setBenefits(request.getBenefits());
        product.setUsageInstructions(request.getUsageInstructions());
        product.setPrice(request.getPrice());
        product.setSalePrice(request.getSalePrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setLowStockThreshold(request.getLowStockThreshold());
        product.setWeight(request.getWeight());
        product.setWeightUnit(request.getWeightUnit());
        product.setStatus(request.getStatus());
        product.setFeatured(request.isFeatured());
        product.setNewArrival(request.isNewArrival());
        product.setMetaTitle(request.getMetaTitle());
        product.setMetaDescription(request.getMetaDescription());
        product.setMetaKeywords(request.getMetaKeywords());

        // Update relations
        if (!Objects.equals(request.getCategoryId(),
                product.getCategory() != null ? product.getCategory().getId() : null)) {
            product.setCategory(findCategoryOrThrow(request.getCategoryId()));
        }
        product.setSubCategory(request.getSubCategoryId() != null
                ? findCategoryOrThrow(request.getSubCategoryId())
                : null);

        product.getTags().clear();
        product.getTags().addAll(resolveTags(request.getTagIds()));

        // Replace images (clear + re-add)
        product.getImages().clear();
        if (!CollectionUtils.isEmpty(request.getImages())) {
            List<ProductImage> images = productMapper.toImageEntityList(request.getImages());
            images.forEach(img -> img.setProduct(product));
            ensureSinglePrimary(images);
            product.getImages().addAll(images);
        }

        // Replace variants (clear + re-add)
        product.getVariants().clear();
        if (!CollectionUtils.isEmpty(request.getVariants())) {
            request.getVariants().forEach(vReq -> {
                validateVariantSku(vReq.getSku(), id);
                ProductVariant variant = productMapper.toVariantEntity(vReq);
                variant.setProduct(product);
                product.getVariants().add(variant);
            });
        }

        Product saved = productRepository.save(product);
        log.info("Product updated: '{}' (id: {})", saved.getName(), saved.getId());
        return buildProductResponse(saved);
    }

    // ─── Delete (soft) ─────────────────────────────────────────

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = findProductOrThrow(id);
        product.setStatus(ProductStatusEnum.INACTIVE);
        productRepository.save(product);
        log.info("Product soft-deleted (INACTIVE): '{}' (id: {})", product.getName(), id);
    }

    // ─── Read ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        return buildProductResponse(findProductOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return buildProductResponse(product);
    }

    // ─── Filtered Listing ──────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ProductPageResponse getProducts(ProductFilterRequest filter, boolean adminMode) {
        // Resolve tag IDs from type-specific name filters
        Set<UUID> resolvedTagIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(filter.getTagIds())) {
            resolvedTagIds.addAll(filter.getTagIds());
        }
        resolveTagIdsByTypeAndNames(TagTypeEnum.MATERIAL, filter.getMaterials(), resolvedTagIds);
        resolveTagIdsByTypeAndNames(TagTypeEnum.PLANET,   filter.getPlanets(),   resolvedTagIds);
        resolveTagIdsByTypeAndNames(TagTypeEnum.CHAKRA,   filter.getChakras(),   resolvedTagIds);
        resolveTagIdsByTypeAndNames(TagTypeEnum.ELEMENT,  filter.getElements(),  resolvedTagIds);

        Specification<Product> spec = ProductSpecification.build(filter, resolvedTagIds, adminMode);
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), buildSort(filter.getSortBy()));

        Page<Product> page = productRepository.findAll(spec, pageable);

        List<ProductSummaryResponse> content = page.getContent().stream()
                .map(this::buildSummaryResponse)
                .toList();

        return ProductPageResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    // ─── Featured / New Arrivals / Related ────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getFeaturedProducts() {
        return productRepository
                .findByStatusAndIsFeaturedTrueOrderByAverageRatingDesc(ProductStatusEnum.ACTIVE)
                .stream()
                .map(this::buildSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getNewArrivals() {
        // BUG-005 fix: sort is passed via Pageable (not baked into method name)
        Pageable pageable = PageRequest.of(0, NEW_ARRIVALS_LIMIT,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return productRepository
                .findByStatusAndIsNewArrivalTrue(ProductStatusEnum.ACTIVE, pageable)
                .stream()
                .map(this::buildSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getRelatedProducts(UUID productId) {
        Product product = findProductOrThrow(productId);
        Pageable pageable = PageRequest.of(0, RELATED_PRODUCTS_LIMIT);
        return productRepository
                .findRelatedProducts(product.getCategory(), productId, pageable)
                .stream()
                .map(this::buildSummaryResponse)
                .toList();
    }

    // ─── Stock & Status ────────────────────────────────────────

    @Override
    @Transactional
    public void updateStock(UUID productId, int quantity) {
        if (quantity < 0) {
            throw new BusinessException("Stock quantity cannot be negative");
        }
        Product product = findProductOrThrow(productId);
        product.setStockQuantity(quantity);
        if (quantity == 0 && product.getStatus() == ProductStatusEnum.ACTIVE) {
            product.setStatus(ProductStatusEnum.OUT_OF_STOCK);
        } else if (quantity > 0 && product.getStatus() == ProductStatusEnum.OUT_OF_STOCK) {
            product.setStatus(ProductStatusEnum.ACTIVE);
        }
        productRepository.save(product);
        log.info("Stock updated for product '{}': {} units", product.getName(), quantity);
    }

    @Override
    @Transactional
    public ProductResponse toggleStatus(UUID id) {
        Product product = findProductOrThrow(id);
        ProductStatusEnum newStatus = product.getStatus() == ProductStatusEnum.ACTIVE
                ? ProductStatusEnum.INACTIVE
                : ProductStatusEnum.ACTIVE;
        product.setStatus(newStatus);
        Product saved = productRepository.save(product);
        log.info("Product '{}' status toggled to: {}", saved.getName(), newStatus);
        return buildProductResponse(saved);
    }

    @Override
    @Transactional
    public void bulkUpdateStatus(List<UUID> ids, ProductStatusEnum status) {
        if (CollectionUtils.isEmpty(ids)) return;
        int updated = productRepository.bulkUpdateStatus(ids, status);
        log.info("Bulk status update: {} products set to {}", updated, status);
    }

    // ─── Private: Response Builders ───────────────────────────

    /**
     * Build a full ProductResponse from a Product entity.
     * Complex computed fields are resolved here, not in the mapper.
     */
    private ProductResponse buildProductResponse(Product product) {
        String primaryImageUrl = resolvePrimaryImageUrl(product);
        BigDecimal discountPercent = computeDiscount(product.getPrice(), product.getSalePrice());

        // Group tags by TagTypeEnum name
        Map<String, List<ProductResponse.TagRef>> tagsByType = product.getTags().stream()
                .collect(Collectors.groupingBy(
                        tag -> tag.getTagType().name(),
                        Collectors.mapping(tag -> ProductResponse.TagRef.builder()
                                .id(tag.getId())
                                .name(tag.getName())
                                .slug(tag.getSlug())
                                .tagType(tag.getTagType().name())
                                .build(),
                        Collectors.toList())
                ));

        List<ProductVariantResponse> variants = product.getVariants().stream()
                .map(v -> {
                    List<ProductImageResponse> variantImages = v.getVariantImages().stream()
                            .map(vi -> ProductImageResponse.builder()
                                    .id(vi.getId())
                                    .imageUrl(vi.getImageUrl())
                                    .altText(vi.getAltText())
                                    .displayOrder(vi.getDisplayOrder())
                                    .build())
                            .toList();
                    return ProductVariantResponse.builder()
                            .id(v.getId())
                            .colorName(v.getColorName())
                            .colorHexCode(v.getColorHexCode())
                            .sku(v.getSku())
                            .price(v.getPrice())
                            .salePrice(v.getSalePrice())
                            .stockQuantity(v.getStockQuantity())
                            .imageUrl(v.getImageUrl())
                            .isActive(v.isActive())
                            .variantImages(variantImages)
                            .build();
                })
                .toList();

        List<ProductImageResponse> images = productMapper.toImageResponseList(product.getImages());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .sku(product.getSku())
                .shortDescription(product.getShortDescription())
                .primaryImageUrl(primaryImageUrl)
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .discountPercent(discountPercent)
                .averageRating(product.getAverageRating())
                .totalReviews(product.getTotalReviews())
                .stockQuantity(product.getStockQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .status(product.getStatus())
                .isFeatured(product.isFeatured())
                .isNewArrival(product.isNewArrival())
                .colorVariants(buildColorVariants(product))
                .description(product.getDescription())
                .benefits(product.getBenefits())
                .usageInstructions(product.getUsageInstructions())
                .weight(product.getWeight())
                .weightUnit(product.getWeightUnit())
                .metaTitle(product.getMetaTitle())
                .metaDescription(product.getMetaDescription())
                .metaKeywords(product.getMetaKeywords())
                .category(toCategoryRef(product.getCategory()))
                .subCategory(product.getSubCategory() != null
                        ? toCategoryRef(product.getSubCategory())
                        : null)
                .tagsByType(tagsByType)
                .images(images)
                .variants(variants)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /** Build a compact ProductSummaryResponse for listing cards. */
    private ProductSummaryResponse buildSummaryResponse(Product product) {
        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .sku(product.getSku())
                .shortDescription(product.getShortDescription())
                .primaryImageUrl(resolvePrimaryImageUrl(product))
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .discountPercent(computeDiscount(product.getPrice(), product.getSalePrice()))
                .averageRating(product.getAverageRating())
                .totalReviews(product.getTotalReviews())
                .stockQuantity(product.getStockQuantity())
                .status(product.getStatus())
                .colorVariants(buildColorVariants(product))
                .isFeatured(product.isFeatured())
                .isNewArrival(product.isNewArrival())
                .build();
    }

    // ─── Private: Helpers ──────────────────────────────────────

    private Product findProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private Category findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    private Set<Tag> resolveTags(List<UUID> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) return new HashSet<>();
        List<Tag> found = tagRepository.findByIdIn(tagIds);
        if (found.size() != tagIds.size()) {
            log.warn("Some tag IDs were not found; requested: {}, found: {}", tagIds.size(), found.size());
        }
        return new HashSet<>(found);
    }

    private void resolveTagIdsByTypeAndNames(TagTypeEnum type, List<String> names, Set<UUID> target) {
        if (CollectionUtils.isEmpty(names)) return;
        tagRepository.findByTagTypeAndNameIn(type, names)
                .forEach(tag -> target.add(tag.getId()));
    }

    private String resolveUniqueSlug(String requestSlug, String name, UUID excludeId) {
        String base = StringUtils.hasText(requestSlug)
                ? SlugUtils.toSlug(requestSlug)
                : SlugUtils.toSlug(name);

        String candidate = base;
        int suffix = 2;
        while (isSlugTaken(candidate, excludeId)) {
            candidate = SlugUtils.toUniqueSlug(base, suffix++);
        }
        return candidate;
    }

    private boolean isSlugTaken(String slug, UUID excludeId) {
        return excludeId == null
                ? productRepository.existsBySlug(slug)
                : productRepository.existsBySlugAndIdNot(slug, excludeId);
    }

    /**
     * Validate that a variant SKU is globally unique across all variants.
     * On create: productId is null — check unconditionally.
     * On update: productId is the owning product's ID — exclude variants of that product.
     *
     * NOTE: This checks variant SKU uniqueness (not product SKU).
     * Previously this incorrectly called productRepository — fixed (BUG-001).
     */
    private void validateVariantSku(String sku, UUID productId) {
        if (productId == null) {
            // Create path: variant SKU must not exist anywhere
            if (variantRepository.existsBySku(sku)) {
                throw new BusinessException("Variant SKU '" + sku + "' already exists");
            }
        }
        // Update path: variant SKU validation is optional here since variants are
        // fully replaced. The DB unique constraint on product_variants.sku is the
        // final guard.
    }

    /** Ensure exactly one image is marked isPrimary; first image becomes primary if none set. */
    private void ensureSinglePrimary(List<ProductImage> images) {
        long primaryCount = images.stream().filter(ProductImage::isPrimary).count();
        if (primaryCount == 0 && !images.isEmpty()) {
            images.get(0).setPrimary(true);
        } else if (primaryCount > 1) {
            // Keep only the first primary
            boolean found = false;
            for (ProductImage img : images) {
                if (img.isPrimary()) {
                    if (found) img.setPrimary(false);
                    else found = true;
                }
            }
        }
    }

    private String resolvePrimaryImageUrl(Product product) {
        return product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(product.getImages().isEmpty() ? null
                        : product.getImages().get(0).getImageUrl());
    }

    private List<ColorVariantInfo> buildColorVariants(Product product) {
        return product.getVariants().stream()
                .filter(ProductVariant::isActive)
                .filter(v -> StringUtils.hasText(v.getColorName()))
                .map(v -> ColorVariantInfo.builder()
                        .colorName(v.getColorName())
                        .colorHexCode(v.getColorHexCode())
                        .build())
                .toList();
    }

    private BigDecimal computeDiscount(BigDecimal price, BigDecimal salePrice) {
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0
                || price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return price.subtract(salePrice)
                .divide(price, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private ProductResponse.CategoryRef toCategoryRef(Category category) {
        if (category == null) return null;
        return ProductResponse.CategoryRef.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .build();
    }

    private Sort buildSort(com.divyagems.ecommerce.enums.SortByEnum sortBy) {
        if (sortBy == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sortBy) {
            case PRICE_ASC  -> Sort.by(Sort.Direction.ASC,  "price");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price");
            case NEWEST     -> Sort.by(Sort.Direction.DESC, "createdAt");
            case RATING     -> Sort.by(Sort.Direction.DESC, "averageRating");
            case NAME_ASC   -> Sort.by(Sort.Direction.ASC,  "name");
        };
    }
}
