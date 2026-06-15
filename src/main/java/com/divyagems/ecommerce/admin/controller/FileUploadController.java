package com.divyagems.ecommerce.admin.controller;

import com.divyagems.ecommerce.common.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Admin-only file upload endpoints.
 * Files are stored on the local filesystem and served at /uploads/**.
 */
@RestController
@RequestMapping("/admin/upload")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — File Upload", description = "Upload images for products, categories, and profiles")
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    // ─── POST /admin/upload ───────────────────────────────────
    @Operation(
            summary = "Upload a single file",
            description = "Accepted types: jpg, jpeg, png, webp. Max size: 10 MB. Subdirectory: products | profiles | categories"
    )
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subdirectory", defaultValue = "products") String subdirectory) {

        String url = fileStorageService.storeFile(file, subdirectory);
        return ResponseEntity.ok(Map.of("url", url));
    }

    // ─── POST /admin/upload/multiple ─────────────────────────
    @Operation(summary = "Upload multiple files in one request")
    @PostMapping(value = "/multiple", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, List<String>>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "subdirectory", defaultValue = "products") String subdirectory) {

        List<String> urls = fileStorageService.storeMultipleFiles(files, subdirectory);
        return ResponseEntity.ok(Map.of("urls", urls));
    }
}
