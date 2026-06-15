package com.divyagems.ecommerce.common.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Abstraction for local filesystem file storage.
 * Files are stored under ${app.upload.dir}/{subdirectory}/ and served at /uploads/{subdirectory}/filename.
 */
public interface FileStorageService {

    /**
     * Store a single file in the given subdirectory.
     *
     * @param file         the uploaded file (JPEG/PNG/WebP, max 10 MB)
     * @param subdirectory e.g. "products", "profiles", "categories"
     * @return relative URL path like "/uploads/products/uuid.jpg"
     */
    String storeFile(MultipartFile file, String subdirectory);

    /**
     * Delete a file by its relative URL ("/uploads/products/uuid.jpg").
     * Silently ignores missing files.
     */
    void deleteFile(String fileUrl);

    /**
     * Store multiple files in the given subdirectory.
     *
     * @return list of relative URL paths, in the same order as the input list
     */
    List<String> storeMultipleFiles(List<MultipartFile> files, String subdirectory);
}
