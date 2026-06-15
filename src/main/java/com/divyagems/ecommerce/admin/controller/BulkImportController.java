package com.divyagems.ecommerce.admin.controller;

import com.divyagems.ecommerce.admin.dto.BulkImportResult;
import com.divyagems.ecommerce.admin.service.BulkImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin endpoints for Excel-based bulk product import.
 */
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Bulk Import", description = "Excel-based bulk product import and template download")
@SecurityRequirement(name = "bearerAuth")
public class BulkImportController {

    private final BulkImportService bulkImportService;

    // ─── POST /admin/products/bulk-import ─────────────────────
    @Operation(
            summary = "Import products from an .xlsx file",
            description = "Columns: Name, SKU, Category, SubCategory, Price, SalePrice, StockQuantity, " +
                    "Description, Benefits, UsageInstructions, Weight, Status, IsFeatured, " +
                    "Stone, Planet, Chakra, Element, VastuUse, ColorVariants. " +
                    "Categories and tags are auto-created if not found. Row errors are collected and returned."
    )
    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkImportResult> importProductsFromExcel(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(bulkImportService.importProductsFromExcel(file));
    }

    // ─── GET /admin/products/bulk-import/template ─────────────
    @Operation(
            summary = "Download sample import template (.xlsx)",
            description = "Returns an Excel file with headers and 2 example rows to guide the import format."
    )
    @GetMapping("/bulk-import/template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        byte[] template = bulkImportService.generateImportTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"product-import-template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(template.length)
                .body(template);
    }
}
