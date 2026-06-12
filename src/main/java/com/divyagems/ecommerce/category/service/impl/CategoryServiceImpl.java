package com.divyagems.ecommerce.category.service.impl;

import com.divyagems.ecommerce.category.dto.CategoryRequest;
import com.divyagems.ecommerce.category.dto.CategoryResponse;
import com.divyagems.ecommerce.category.dto.CategoryTreeResponse;
import com.divyagems.ecommerce.category.mapper.CategoryMapper;
import com.divyagems.ecommerce.category.repository.CategoryRepository;
import com.divyagems.ecommerce.category.service.CategoryService;
import com.divyagems.ecommerce.common.SlugUtils;
import com.divyagems.ecommerce.entity.Category;
import com.divyagems.ecommerce.exception.BusinessException;
import com.divyagems.ecommerce.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    // ─── Create ────────────────────────────────────────────────

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        // 1. Resolve and deduplicate slug
        String slug = resolveUniqueSlug(request, null);

        // 2. Resolve parent (if provided)
        Category parent = resolveParent(request.getParentId());

        // 3. Map request to entity
        Category category = categoryMapper.toEntity(request);
        category.setSlug(slug);
        category.setParent(parent);

        Category saved = categoryRepository.save(category);
        log.info("Category created: '{}' (slug: {})", saved.getName(), saved.getSlug());

        return buildResponse(saved, fetchProductCountMap());
    }

    // ─── Update ────────────────────────────────────────────────

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = findCategoryOrThrow(id);

        // Re-generate slug only if name changed and no explicit slug given
        boolean nameChanged = !category.getName().equalsIgnoreCase(request.getName());
        boolean explicitSlug = StringUtils.hasText(request.getSlug())
                && !request.getSlug().equals(category.getSlug());

        if (nameChanged || explicitSlug) {
            String newSlug = resolveUniqueSlug(request, id);
            category.setSlug(newSlug);
        }

        // Resolve parent change
        if (!Objects.equals(
                request.getParentId(),
                category.getParent() != null ? category.getParent().getId() : null)) {

            // Guard: prevent circular hierarchy (setting own child as parent)
            if (request.getParentId() != null && request.getParentId().equals(id)) {
                throw new BusinessException("A category cannot be its own parent");
            }
            category.setParent(resolveParent(request.getParentId()));
        }

        // Apply remaining fields
        categoryMapper.updateFromRequest(request, category);

        Category saved = categoryRepository.save(category);
        log.info("Category updated: '{}' (id: {})", saved.getName(), saved.getId());

        return buildResponse(saved, fetchProductCountMap());
    }

    // ─── Delete ────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = findCategoryOrThrow(id);

        // Guard: must not have products assigned
        if (categoryRepository.hasProducts(id)) {
            throw new BusinessException(
                    "Cannot delete category '" + category.getName() + "' — it has associated products. " +
                    "Reassign or delete the products first.");
        }

        // Guard: must not have subcategories
        if (categoryRepository.hasChildren(id)) {
            throw new BusinessException(
                    "Cannot delete category '" + category.getName() + "' — it has subcategories. " +
                    "Delete or reassign subcategories first.");
        }

        categoryRepository.delete(category);
        log.info("Category deleted: '{}' (id: {})", category.getName(), id);
    }

    // ─── Read ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = findCategoryOrThrow(id);
        return buildResponse(category, fetchProductCountMap());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return buildResponse(category, fetchProductCountMap());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        Map<UUID, Long> productCounts = fetchProductCountMap();
        return categories.stream()
                .map(c -> buildResponse(c, productCounts))
                .toList();
    }

    // ─── Category Tree ─────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree() {
        // Fetch top-level categories only
        List<Category> topLevel = categoryRepository
                .findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();

        Map<UUID, Long> productCounts = fetchProductCountMap();

        return topLevel.stream()
                .map(cat -> buildTreeResponse(cat, productCounts))
                .toList();
    }

    // ─── Toggle Status ─────────────────────────────────────────

    @Override
    @Transactional
    public CategoryResponse toggleStatus(UUID id) {
        Category category = findCategoryOrThrow(id);
        boolean newStatus = !category.isActive();
        category.setActive(newStatus);

        Category saved = categoryRepository.save(category);
        log.info("Category '{}' status toggled to: {}", saved.getName(), newStatus ? "ACTIVE" : "INACTIVE");

        return buildResponse(saved, fetchProductCountMap());
    }

    // ─── Private Helpers ───────────────────────────────────────

    private Category findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    private Category resolveParent(UUID parentId) {
        if (parentId == null) return null;
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category", "id", parentId));
    }

    /**
     * Generate a unique slug from the request.
     * Priority: explicit slug from request → auto-generated from name.
     * Appends numeric suffix if slug already exists (e.g. "ruby-gems-2").
     *
     * @param excludeId the category ID to exclude from uniqueness check (for updates), null for creates
     */
    private String resolveUniqueSlug(CategoryRequest request, UUID excludeId) {
        String base = StringUtils.hasText(request.getSlug())
                ? SlugUtils.toSlug(request.getSlug())
                : SlugUtils.toSlug(request.getName());

        String candidate = base;
        int suffix = 2;

        while (isSlugTaken(candidate, excludeId)) {
            candidate = SlugUtils.toUniqueSlug(base, suffix++);
        }
        return candidate;
    }

    private boolean isSlugTaken(String slug, UUID excludeId) {
        if (excludeId == null) {
            return categoryRepository.existsBySlug(slug);
        }
        return categoryRepository.existsBySlugAndIdNot(slug, excludeId);
    }

    /**
     * Fetch product counts for ALL categories as a Map<categoryId, count>.
     * Single query to avoid N+1 when rendering lists.
     */
    private Map<UUID, Long> fetchProductCountMap() {
        List<Object[]> rows = categoryRepository.findAllWithProductCount();
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            UUID categoryId = (UUID) row[0];
            Long count = ((Number) row[1]).longValue();
            map.put(categoryId, count);
        }
        return map;
    }

    /**
     * Build a CategoryResponse from an entity, injecting productCount and children.
     */
    private CategoryResponse buildResponse(Category category, Map<UUID, Long> productCounts) {
        CategoryResponse response = categoryMapper.toResponse(category);

        // Inject product count from pre-fetched map
        long count = productCounts.getOrDefault(category.getId(), 0L);

        // Inject direct children (one level deep)
        List<CategoryResponse> children = category.getChildren().stream()
                .map(child -> {
                    CategoryResponse childResp = categoryMapper.toResponse(child);
                    long childCount = productCounts.getOrDefault(child.getId(), 0L);
                    return CategoryResponse.builder()
                            .id(childResp.getId())
                            .name(childResp.getName())
                            .slug(childResp.getSlug())
                            .description(childResp.getDescription())
                            .imageUrl(childResp.getImageUrl())
                            .displayOrder(childResp.getDisplayOrder())
                            .isActive(childResp.isActive())
                            .productCount(childCount)
                            .parentId(childResp.getParentId())
                            .parentName(childResp.getParentName())
                            .children(List.of())
                            .build();
                })
                .toList();

        return CategoryResponse.builder()
                .id(response.getId())
                .name(response.getName())
                .slug(response.getSlug())
                .description(response.getDescription())
                .imageUrl(response.getImageUrl())
                .displayOrder(response.getDisplayOrder())
                .isActive(response.isActive())
                .productCount(count)
                .parentId(response.getParentId())
                .parentName(response.getParentName())
                .children(children)
                .build();
    }

    /**
     * Build a CategoryTreeResponse recursively — each node contains nested children.
     */
    private CategoryTreeResponse buildTreeResponse(Category category, Map<UUID, Long> productCounts) {
        List<CategoryTreeResponse> childResponses = category.getChildren().stream()
                .filter(Category::isActive)
                .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                .map(child -> buildTreeResponse(child, productCounts))
                .toList();

        long count = productCounts.getOrDefault(category.getId(), 0L);

        return CategoryTreeResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.isActive())
                .productCount(count)
                .children(childResponses)
                .build();
    }
}
