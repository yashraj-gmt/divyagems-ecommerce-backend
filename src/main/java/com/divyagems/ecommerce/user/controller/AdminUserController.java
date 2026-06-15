package com.divyagems.ecommerce.user.controller;

import com.divyagems.ecommerce.user.dto.response.UserProfileResponse;
import com.divyagems.ecommerce.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin-only user management endpoints.
 * Protected by both the /admin/** path rule in SecurityConfig and @PreAuthorize.
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Users", description = "Admin user management: list, view, activate/deactivate")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    // ─── GET /admin/users ─────────────────────────────────────
    @Operation(
            summary = "List all users (paginated)",
            description = "Filter by search term (email / name) and/or active status."
    )
    @GetMapping
    public ResponseEntity<Page<UserProfileResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(adminUserService.getAllUsers(search, isActive, pageable));
    }

    // ─── GET /admin/users/{id} ────────────────────────────────
    @Operation(summary = "Get full profile of a specific user (admin)")
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    // ─── PATCH /admin/users/{id}/toggle-active ────────────────
    @Operation(
            summary = "Toggle user active/inactive",
            description = "Deactivating revokes all refresh tokens immediately."
    )
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<UserProfileResponse> toggleUserActive(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.toggleUserActive(id));
    }
}
