package com.divyagems.ecommerce.common.service.impl;

import com.divyagems.ecommerce.common.service.FileStorageService;
import com.divyagems.ecommerce.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Local filesystem implementation of FileStorageService.
 *
 * Storage layout:
 *   {uploadDir}/products/   → product images
 *   {uploadDir}/profiles/   → user profile images
 *   {uploadDir}/categories/ → category images
 *
 * URLs are served at /uploads/{subdirectory}/{filename} via WebMvcConfig.
 */
@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final long MAX_BYTES        = 10L * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED   = Set.of("jpg", "jpeg", "png", "webp");

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        // Pre-create known subdirectories on startup
        for (String sub : List.of("products", "profiles", "categories")) {
            createSubdirectory(sub);
        }
    }

    // ─── Store Single File ──────────────────────────────────────

    @Override
    public String storeFile(MultipartFile file, String subdirectory) {
        validateFile(file);

        String ext      = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;
        Path   dir      = resolveDir(subdirectory);
        Path   target   = dir.resolve(filename);

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file {} in {}: {}", filename, subdirectory, e.getMessage());
            throw new BusinessException("Could not store file. Please try again.");
        }

        String url = "/uploads/" + subdirectory + "/" + filename;
        log.info("Stored file: {}", url);
        return url;
    }

    // ─── Delete File ────────────────────────────────────────────

    @Override
    public void deleteFile(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) return;

        // Convert "/uploads/products/abc.jpg" → absolute path
        String relative = fileUrl.replaceFirst("^/uploads/", "");
        Path   target   = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(relative);

        try {
            if (Files.exists(target)) {
                Files.delete(target);
                log.info("Deleted file: {}", target);
            }
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", target, e.getMessage());
        }
    }

    // ─── Store Multiple Files ───────────────────────────────────

    @Override
    public List<String> storeMultipleFiles(List<MultipartFile> files, String subdirectory) {
        return files.stream()
                .map(f -> storeFile(f, subdirectory))
                .collect(Collectors.toList());
    }

    // ─── Private Helpers ───────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File must not be empty.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(
                    "File size exceeds the 10 MB limit (got "
                    + (file.getSize() / (1024 * 1024)) + " MB).");
        }
        String ext = getExtension(file.getOriginalFilename());
        if (!ALLOWED.contains(ext)) {
            throw new BusinessException(
                    "File type '." + ext + "' is not allowed. Allowed types: jpg, jpeg, png, webp.");
        }
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new BusinessException("File must have a valid extension (jpg, jpeg, png, webp).");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private Path resolveDir(String subdirectory) {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(subdirectory);
        createSubdirectory(subdirectory);
        return dir;
    }

    private void createSubdirectory(String subdirectory) {
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(subdirectory);
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.warn("Could not create upload directory '{}': {}", subdirectory, e.getMessage());
        }
    }
}
