package com.divyagems.ecommerce.user.controller;

import com.divyagems.ecommerce.auth.dto.response.MessageResponse;
import com.divyagems.ecommerce.user.dto.request.AddressRequest;
import com.divyagems.ecommerce.user.dto.request.ChangePasswordRequest;
import com.divyagems.ecommerce.user.dto.request.UpdateProfileRequest;
import com.divyagems.ecommerce.user.dto.response.AddressResponse;
import com.divyagems.ecommerce.user.dto.response.UserProfileResponse;
import com.divyagems.ecommerce.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Customer-facing user profile and address management endpoints.
 * All endpoints require a valid JWT — secured at /users/** in SecurityConfig.
 */
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Manage your profile, password, and saved addresses")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final ProfileService profileService;

    // ─── GET /users/me ────────────────────────────────────────
    @Operation(summary = "Get current user's profile")
    @GetMapping
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        return ResponseEntity.ok(profileService.getMyProfile());
    }

    // ─── PUT /users/me ────────────────────────────────────────
    @Operation(summary = "Update profile (firstName, lastName, phone, alternatePhone)")
    @PutMapping
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateMyProfile(request));
    }

    // ─── POST /users/me/profile-image ─────────────────────────
    @Operation(summary = "Upload a new profile picture (JPEG/PNG/WebP, max 10 MB)")
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> uploadProfileImage(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(profileService.uploadProfileImage(file));
    }

    // ─── POST /users/me/change-password ───────────────────────
    @Operation(summary = "Change password — requires current password verification")
    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(profileService.changePassword(request));
    }

    // ─── GET /users/me/addresses ──────────────────────────────
    @Operation(summary = "List all saved addresses (default address first)")
    @GetMapping("/addresses")
    public ResponseEntity<List<AddressResponse>> getMyAddresses() {
        return ResponseEntity.ok(profileService.getMyAddresses());
    }

    // ─── POST /users/me/addresses ─────────────────────────────
    @Operation(summary = "Add a new address to the address book")
    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> addAddress(@Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(profileService.addAddress(request));
    }

    // ─── PUT /users/me/addresses/{id} ─────────────────────────
    @Operation(summary = "Update a saved address")
    @PutMapping("/addresses/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(profileService.updateAddress(id, request));
    }

    // ─── DELETE /users/me/addresses/{id} ──────────────────────
    @Operation(summary = "Delete a saved address")
    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID id) {
        profileService.deleteAddress(id);
        return ResponseEntity.noContent().build();
    }

    // ─── PATCH /users/me/addresses/{id}/default ───────────────
    @Operation(summary = "Set an address as the default shipping address")
    @PatchMapping("/addresses/{id}/default")
    public ResponseEntity<AddressResponse> setDefaultAddress(@PathVariable UUID id) {
        return ResponseEntity.ok(profileService.setDefaultAddress(id));
    }
}
