package com.divyagems.ecommerce.admin.service.impl;

import com.divyagems.ecommerce.admin.dto.BulkImportResult;
import com.divyagems.ecommerce.admin.service.BulkImportService;
import com.divyagems.ecommerce.category.repository.CategoryRepository;
import com.divyagems.ecommerce.common.SlugUtils;
import com.divyagems.ecommerce.entity.*;
import com.divyagems.ecommerce.enums.ProductStatusEnum;
import com.divyagems.ecommerce.enums.TagTypeEnum;
import com.divyagems.ecommerce.exception.BusinessException;
import com.divyagems.ecommerce.product.repository.ProductRepository;
import com.divyagems.ecommerce.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Excel bulk product import service.
 *
 * Expected column order (0-indexed):
 *  0  Name            - required
 *  1  SKU             - required, unique
 *  2  Category        - required, auto-created if absent
 *  3  SubCategory     - optional, auto-created under Category if absent
 *  4  Price           - required, numeric
 *  5  SalePrice       - optional, numeric
 *  6  StockQuantity   - required, integer
 *  7  Description     - optional
 *  8  Benefits        - optional
 *  9  UsageInstructions - optional
 *  10 Weight          - optional, decimal
 *  11 Status          - optional (ACTIVE|INACTIVE|DRAFT), defaults to DRAFT
 *  12 IsFeatured      - optional (true|false)
 *  13 Stone           - optional tag (MATERIAL type), auto-created
 *  14 Planet          - optional tag (PLANET type), auto-created
 *  15 Chakra          - optional tag (CHAKRA type), auto-created
 *  16 Element         - optional tag (ELEMENT type), auto-created
 *  17 VastuUse        - optional tag (VASTU_USE type), auto-created
 *  18 ColorVariants   - optional, format: "Red:#FF0000:100:50, Blue:#0000FF:200:75"
 *                        (colorName:hexCode:stockQty:additionalPrice)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkImportServiceImpl implements BulkImportService {

    private static final int COL_NAME        = 0;
    private static final int COL_SKU         = 1;
    private static final int COL_CATEGORY    = 2;
    private static final int COL_SUBCATEGORY = 3;
    private static final int COL_PRICE       = 4;
    private static final int COL_SALE_PRICE  = 5;
    private static final int COL_STOCK       = 6;
    private static final int COL_DESCRIPTION = 7;
    private static final int COL_BENEFITS    = 8;
    private static final int COL_USAGE       = 9;
    private static final int COL_WEIGHT      = 10;
    private static final int COL_STATUS      = 11;
    private static final int COL_FEATURED    = 12;
    private static final int COL_STONE       = 13;
    private static final int COL_PLANET      = 14;
    private static final int COL_CHAKRA      = 15;
    private static final int COL_ELEMENT     = 16;
    private static final int COL_VASTU       = 17;
    private static final int COL_VARIANTS    = 18;

    private final ProductRepository  productRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository      tagRepository;

    // ─── Import ────────────────────────────────────────────────

    @Override
    @Transactional
    public BulkImportResult importProductsFromExcel(MultipartFile file) {
        validateExcelFile(file);

        int imported = 0;
        int skipped  = 0;
        List<BulkImportResult.RowError> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int   totalRows = sheet.getLastRowNum();

            // Row 0 = headers, data starts at row 1
            for (int rowIdx = 1; rowIdx <= totalRows; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isRowEmpty(row)) {
                    skipped++;
                    continue;
                }

                try {
                    processRow(row, rowIdx + 1); // +1 for 1-based display
                    imported++;
                } catch (Exception e) {
                    log.warn("Row {} failed: {}", rowIdx + 1, e.getMessage());
                    errors.add(BulkImportResult.RowError.builder()
                            .row(rowIdx + 1)
                            .error(e.getMessage())
                            .build());
                    skipped++;
                }
            }
        } catch (IOException e) {
            throw new BusinessException("Could not parse the Excel file: " + e.getMessage());
        }

        log.info("Bulk import complete — imported: {}, skipped: {}, errors: {}", imported, skipped, errors.size());
        return BulkImportResult.builder()
                .imported(imported)
                .skipped(skipped)
                .errors(errors)
                .build();
    }

    // ─── Template Generator ────────────────────────────────────

    @Override
    public byte[] generateImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");

            // ── Header row ──
            String[] headers = {
                "Name", "SKU", "Category", "SubCategory",
                "Price", "SalePrice", "StockQuantity",
                "Description", "Benefits", "UsageInstructions",
                "Weight", "Status", "IsFeatured",
                "Stone", "Planet", "Chakra", "Element", "VastuUse",
                "ColorVariants"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // ── Example row 1 ──
            Row row1 = sheet.createRow(1);
            row1.createCell(COL_NAME).setCellValue("Ruby Healing Bracelet");
            row1.createCell(COL_SKU).setCellValue("SKU-RUBY-001");
            row1.createCell(COL_CATEGORY).setCellValue("Gemstones");
            row1.createCell(COL_SUBCATEGORY).setCellValue("Bracelets");
            row1.createCell(COL_PRICE).setCellValue(1499.00);
            row1.createCell(COL_SALE_PRICE).setCellValue(1299.00);
            row1.createCell(COL_STOCK).setCellValue(50);
            row1.createCell(COL_DESCRIPTION).setCellValue("Natural Ruby healing bracelet for Sun energy.");
            row1.createCell(COL_BENEFITS).setCellValue("Confidence, Courage, Vitality");
            row1.createCell(COL_USAGE).setCellValue("Wear on right wrist on Sundays.");
            row1.createCell(COL_WEIGHT).setCellValue(45.5);
            row1.createCell(COL_STATUS).setCellValue("ACTIVE");
            row1.createCell(COL_FEATURED).setCellValue("true");
            row1.createCell(COL_STONE).setCellValue("Ruby");
            row1.createCell(COL_PLANET).setCellValue("Sun");
            row1.createCell(COL_CHAKRA).setCellValue("Root Chakra");
            row1.createCell(COL_ELEMENT).setCellValue("Fire");
            row1.createCell(COL_VASTU).setCellValue("South Zone");
            row1.createCell(COL_VARIANTS).setCellValue("Red:#FF0000:30:0, Pink:#FF69B4:20:100");

            // ── Example row 2 ──
            Row row2 = sheet.createRow(2);
            row2.createCell(COL_NAME).setCellValue("Amethyst Pendant");
            row2.createCell(COL_SKU).setCellValue("SKU-AME-002");
            row2.createCell(COL_CATEGORY).setCellValue("Gemstones");
            row2.createCell(COL_SUBCATEGORY).setCellValue("Pendants");
            row2.createCell(COL_PRICE).setCellValue(899.00);
            row2.createCell(COL_SALE_PRICE).setCellValue("");
            row2.createCell(COL_STOCK).setCellValue(100);
            row2.createCell(COL_DESCRIPTION).setCellValue("Natural Amethyst pendant for clarity and peace.");
            row2.createCell(COL_BENEFITS).setCellValue("Calm, Clarity, Spiritual Growth");
            row2.createCell(COL_USAGE).setCellValue("Wear around neck or meditate holding it.");
            row2.createCell(COL_WEIGHT).setCellValue(15.0);
            row2.createCell(COL_STATUS).setCellValue("ACTIVE");
            row2.createCell(COL_FEATURED).setCellValue("false");
            row2.createCell(COL_STONE).setCellValue("Amethyst");
            row2.createCell(COL_PLANET).setCellValue("Jupiter");
            row2.createCell(COL_CHAKRA).setCellValue("Crown Chakra");
            row2.createCell(COL_ELEMENT).setCellValue("Air");
            row2.createCell(COL_VASTU).setCellValue("");
            row2.createCell(COL_VARIANTS).setCellValue("");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new BusinessException("Could not generate import template: " + e.getMessage());
        }
    }

    // ─── Row Processing ────────────────────────────────────────

    private void processRow(Row row, int displayRowNum) {
        // 1. Required fields
        String name = getString(row, COL_NAME);
        if (!StringUtils.hasText(name))
            throw new BusinessException("Name is required");

        String sku = getString(row, COL_SKU);
        if (!StringUtils.hasText(sku))
            throw new BusinessException("SKU is required");

        String categoryName = getString(row, COL_CATEGORY);
        if (!StringUtils.hasText(categoryName))
            throw new BusinessException("Category is required");

        String priceStr = getString(row, COL_PRICE);
        if (!StringUtils.hasText(priceStr))
            throw new BusinessException("Price is required");

        // 2. Duplicate SKU check
        if (productRepository.existsBySku(sku.trim()))
            throw new BusinessException("SKU '" + sku.trim() + "' already exists — skipped");

        // 3. Parse numeric fields
        BigDecimal price;
        try {
            price = new BigDecimal(priceStr.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("Invalid price: '" + priceStr + "'");
        }

        String stockStr = getString(row, COL_STOCK);
        int stockQty = 0;
        if (StringUtils.hasText(stockStr)) {
            try { stockQty = Integer.parseInt(stockStr.trim()); }
            catch (NumberFormatException e) { throw new BusinessException("Invalid StockQuantity: '" + stockStr + "'"); }
        }

        BigDecimal salePrice = null;
        String salePriceStr = getString(row, COL_SALE_PRICE);
        if (StringUtils.hasText(salePriceStr)) {
            try { salePrice = new BigDecimal(salePriceStr.trim()); }
            catch (NumberFormatException e) { /* silently ignore bad salePrice */ }
        }

        Double weight = null;
        String weightStr = getString(row, COL_WEIGHT);
        if (StringUtils.hasText(weightStr)) {
            try { weight = Double.parseDouble(weightStr.trim()); }
            catch (NumberFormatException e) { /* ignore */ }
        }

        // 4. Status
        ProductStatusEnum status = ProductStatusEnum.DRAFT;
        String statusStr = getString(row, COL_STATUS);
        if (StringUtils.hasText(statusStr)) {
            try { status = ProductStatusEnum.valueOf(statusStr.trim().toUpperCase()); }
            catch (IllegalArgumentException e) { /* default to DRAFT */ }
        }

        boolean isFeatured = "true".equalsIgnoreCase(getString(row, COL_FEATURED));

        // 5. Resolve / auto-create Category
        Category category = resolveOrCreateCategory(categoryName.trim(), null);

        // 6. Resolve / auto-create SubCategory (child of Category)
        Category subCategory = null;
        String subCategoryName = getString(row, COL_SUBCATEGORY);
        if (StringUtils.hasText(subCategoryName)) {
            subCategory = resolveOrCreateCategory(subCategoryName.trim(), category);
        }

        // 7. Generate unique slug
        String baseSlug = SlugUtils.toSlug(name.trim());
        String slug     = baseSlug;
        int    suffix   = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = SlugUtils.toUniqueSlug(baseSlug, suffix++);
        }

        // 8. Build product
        Product product = Product.builder()
                .name(name.trim())
                .slug(slug)
                .sku(sku.trim())
                .price(price)
                .salePrice(salePrice)
                .stockQuantity(stockQty)
                .description(getString(row, COL_DESCRIPTION))
                .benefits(getString(row, COL_BENEFITS))
                .usageInstructions(getString(row, COL_USAGE))
                .weight(weight)
                .status(status)
                .isFeatured(isFeatured)
                .category(category)
                .subCategory(subCategory)
                .build();

        // 9. Tags
        Set<Tag> tags = new HashSet<>();
        addTagIfPresent(tags, row, COL_STONE,  TagTypeEnum.MATERIAL);
        addTagIfPresent(tags, row, COL_PLANET, TagTypeEnum.PLANET);
        addTagIfPresent(tags, row, COL_CHAKRA, TagTypeEnum.CHAKRA);
        addTagIfPresent(tags, row, COL_ELEMENT,TagTypeEnum.ELEMENT);
        addTagIfPresent(tags, row, COL_VASTU,  TagTypeEnum.VASTU_USE);
        product.setTags(tags);

        // 10. Color variants
        String variantStr = getString(row, COL_VARIANTS);
        if (StringUtils.hasText(variantStr)) {
            List<ProductVariant> variants = parseVariants(variantStr, product);
            product.setVariants(variants);
        }

        productRepository.save(product);
        log.debug("Row {} imported: {} ({})", displayRowNum, name, sku);
    }

    // ─── Private Helpers ───────────────────────────────────────

    /**
     * Resolve an existing category by name, or create it if not found.
     * parent=null → top-level; parent set → sub-category.
     */
    private Category resolveOrCreateCategory(String name, Category parent) {
        return categoryRepository.findByName(name).orElseGet(() -> {
            String slug     = SlugUtils.toSlug(name);
            String baseSlug = slug;
            int    suffix   = 1;
            while (categoryRepository.existsBySlug(slug)) {
                slug = SlugUtils.toUniqueSlug(baseSlug, suffix++);
            }
            Category cat = Category.builder()
                    .name(name)
                    .slug(slug)
                    .parent(parent)
                    .isActive(true)
                    .build();
            return categoryRepository.save(cat);
        });
    }

    /**
     * Add a tag to the set, creating it if it doesn't exist.
     */
    private void addTagIfPresent(Set<Tag> tags, Row row, int col, TagTypeEnum type) {
        String name = getString(row, col);
        if (!StringUtils.hasText(name)) return;
        String tagName = name.trim();
        Tag tag = tagRepository.findByTagTypeAndName(type, tagName).orElseGet(() -> {
            String baseSlug = SlugUtils.toSlug(tagName);
            String slug     = baseSlug;
            int    suffix   = 1;
            // Ensure slug is unique across all tags
            while (tagRepository.findByName(slug).isPresent()) {
                slug = SlugUtils.toUniqueSlug(baseSlug, suffix++);
            }
            return tagRepository.save(Tag.builder()
                    .name(tagName)
                    .slug(slug)
                    .tagType(type)
                    .build());
        });
        tags.add(tag);
    }

    /**
     * Parse color variant string: "Red:#FF0000:100:50, Blue:#0000FF:200:0"
     * Format: colorName:hexCode:stockQty:additionalPrice
     */
    private List<ProductVariant> parseVariants(String raw, Product product) {
        List<ProductVariant> variants = new ArrayList<>();
        String[] parts = raw.split(",");
        for (String part : parts) {
            part = part.trim();
            if (!StringUtils.hasText(part)) continue;
            String[] tokens = part.split(":");
            if (tokens.length < 2) continue;

            String  colorName  = tokens[0].trim();
            String  hexCode    = tokens.length > 1 ? tokens[1].trim() : null;
            int     stock      = 0;
            BigDecimal variantPrice = null;

            try { if (tokens.length > 2) stock        = Integer.parseInt(tokens[2].trim()); } catch (Exception ignored) {}
            try { if (tokens.length > 3 && StringUtils.hasText(tokens[3].trim())) {
                BigDecimal add = new BigDecimal(tokens[3].trim());
                // additionalPrice treated as absolute override price if > 0
                if (add.compareTo(BigDecimal.ZERO) > 0) variantPrice = product.getPrice().add(add);
            }} catch (Exception ignored) {}

            // Generate variant SKU
            String variantSku = product.getSku() + "-" + colorName.toUpperCase().replace(" ", "");

            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .colorName(colorName)
                    .colorHexCode(hexCode)
                    .stockQuantity(stock)
                    .price(variantPrice)
                    .sku(variantSku)
                    .isActive(true)
                    .build();
            variants.add(variant);
        }
        return variants;
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Upload file must not be empty.");
        }
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename) || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException("Only .xlsx files are supported for bulk import.");
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && StringUtils.hasText(getCellStringValue(cell))) {
                return false;
            }
        }
        return true;
    }

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return getCellStringValue(cell).trim();
    }

    private String getCellStringValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                // Return integer string if it's a whole number
                if (d == Math.floor(d) && !Double.isInfinite(d))
                    yield String.valueOf((long) d);
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> "";
        };
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }
}
