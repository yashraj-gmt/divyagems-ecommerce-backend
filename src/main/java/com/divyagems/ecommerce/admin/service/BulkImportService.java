package com.divyagems.ecommerce.admin.service;

import com.divyagems.ecommerce.admin.dto.BulkImportResult;
import org.springframework.web.multipart.MultipartFile;

public interface BulkImportService {

    /**
     * Parse an .xlsx file and import products row by row.
     * Row-level errors are collected and returned rather than aborting the whole import.
     * Categories, sub-categories, and tags are auto-created if not found.
     */
    BulkImportResult importProductsFromExcel(MultipartFile file);

    /**
     * Generate and return a sample .xlsx template with headers and 2 example rows
     * so operators know the expected column format.
     *
     * @return raw byte array of the OOXML workbook
     */
    byte[] generateImportTemplate();
}
