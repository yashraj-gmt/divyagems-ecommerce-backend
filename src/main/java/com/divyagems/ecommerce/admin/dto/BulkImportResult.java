package com.divyagems.ecommerce.admin.dto;

import lombok.*;

import java.util.List;

/**
 * Result of an Excel bulk product import operation.
 * Errors include row number + reason so the operator can fix the spreadsheet.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkImportResult {

    private int imported;
    private int skipped;
    private List<RowError> errors;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int    row;
        private String error;
    }
}
