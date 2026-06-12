package com.divyagems.ecommerce.category.mapper;

import com.divyagems.ecommerce.category.dto.CategoryRequest;
import com.divyagems.ecommerce.category.dto.CategoryResponse;
import com.divyagems.ecommerce.category.dto.CategoryTreeResponse;
import com.divyagems.ecommerce.entity.Category;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for the Category domain.
 *
 * Notes:
 * - productCount and children are NOT mapped automatically; they are set by
 *   CategoryServiceImpl after the initial mapping (via @AfterMapping or explicit setters).
 * - Parent info (parentId, parentName) is mapped from category.parent.
 * - @BeanMapping(nullValuePropertyMappingStrategy = IGNORE) on updateFromRequest
 *   means null fields in the request leave the entity field unchanged.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    // ─── Entity → CategoryResponse ──────────────────────────

    @Mapping(target = "parentId",   source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    @Mapping(target = "productCount", ignore = true)   // set in service
    @Mapping(target = "children",   ignore = true)      // set in service
    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponseList(List<Category> categories);

    // ─── Entity → CategoryTreeResponse ──────────────────────

    @Mapping(target = "productCount", ignore = true)   // set in service
    @Mapping(target = "children",     ignore = true)   // set recursively in service
    CategoryTreeResponse toTreeResponse(Category category);

    List<CategoryTreeResponse> toTreeResponseList(List<Category> categories);

    // ─── CategoryRequest → Entity (CREATE) ──────────────────

    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "slug",       ignore = true)   // generated in service
    @Mapping(target = "parent",     ignore = true)   // resolved in service
    @Mapping(target = "children",   ignore = true)
    @Mapping(target = "products",   ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "updatedAt",  ignore = true)
    @Mapping(target = "createdBy",  ignore = true)
    @Mapping(target = "updatedBy",  ignore = true)
    Category toEntity(CategoryRequest request);

    // ─── CategoryRequest → Entity (UPDATE) ──────────────────

    /**
     * Applies non-null request fields onto an existing Category entity.
     * Null values in the request are ignored, preserving current entity state.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "slug",       ignore = true)   // re-generated in service if name changed
    @Mapping(target = "parent",     ignore = true)   // resolved in service
    @Mapping(target = "children",   ignore = true)
    @Mapping(target = "products",   ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "updatedAt",  ignore = true)
    @Mapping(target = "createdBy",  ignore = true)
    @Mapping(target = "updatedBy",  ignore = true)
    void updateFromRequest(CategoryRequest request, @MappingTarget Category category);
}
