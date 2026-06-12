package com.divyagems.ecommerce.product.mapper;

import com.divyagems.ecommerce.entity.ProductImage;
import com.divyagems.ecommerce.entity.ProductVariant;
import com.divyagems.ecommerce.entity.ProductVariantImage;
import com.divyagems.ecommerce.product.dto.request.ProductImageRequest;
import com.divyagems.ecommerce.product.dto.request.ProductVariantRequest;
import com.divyagems.ecommerce.product.dto.response.ProductImageResponse;
import com.divyagems.ecommerce.product.dto.response.ProductVariantResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for Product sub-entities (variants and images).
 *
 * ProductSummaryResponse and ProductResponse are built manually in
 * ProductServiceImpl using the builder pattern, because they contain
 * complex computed fields (discountPercent, primaryImageUrl, tagsByType)
 * that are cleaner to express in Java than via MapStruct annotations.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    // ─── Variant: Request → Entity ─────────────────────────────

    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "product",    ignore = true)
    @Mapping(target = "variantImages", ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "updatedAt",  ignore = true)
    @Mapping(target = "createdBy",  ignore = true)
    @Mapping(target = "updatedBy",  ignore = true)
    ProductVariant toVariantEntity(ProductVariantRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "product",    ignore = true)
    @Mapping(target = "variantImages", ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "updatedAt",  ignore = true)
    @Mapping(target = "createdBy",  ignore = true)
    @Mapping(target = "updatedBy",  ignore = true)
    void updateVariantFromRequest(ProductVariantRequest request, @MappingTarget ProductVariant variant);

    // ─── Variant: Entity → Response ───────────────────────────

    @Mapping(target = "variantImages", source = "variantImages")
    ProductVariantResponse toVariantResponse(ProductVariant variant);

    List<ProductVariantResponse> toVariantResponseList(List<ProductVariant> variants);

    // ─── Image: Request → Entity ───────────────────────────────

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "product",   ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ProductImage toImageEntity(ProductImageRequest request);

    List<ProductImage> toImageEntityList(List<ProductImageRequest> requests);

    // ─── Image: Entity → Response ──────────────────────────────

    ProductImageResponse toImageResponse(ProductImage image);

    List<ProductImageResponse> toImageResponseList(List<ProductImage> images);

    // ─── Variant Image: Entity → Response ─────────────────────

    @Mapping(target = "isPrimary", ignore = true)   // variant images have no isPrimary
    ProductImageResponse toVariantImageResponse(ProductVariantImage variantImage);
}
