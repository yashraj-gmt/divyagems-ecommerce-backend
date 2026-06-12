package com.divyagems.ecommerce.auth.controller;

import com.divyagems.ecommerce.auth.dto.request.*;
import com.divyagems.ecommerce.auth.dto.response.AuthResponse;
import com.divyagems.ecommerce.auth.dto.response.MessageResponse;
import com.divyagems.ecommerce.auth.dto.response.UserProfileResponse;
import com.divyagems.ecommerce.auth.service.AuthService;
import com.divyagems.ecommerce.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for all authentication and authorization operations.
 *
 * Base path: /auth  (full URL: {context-path}/auth)
 * Context path is /api/v1, so full base is: /api/v1/auth
 *
 * Public endpoints (no JWT required):
 *   POST /auth/register, /auth/login, /auth/refresh-token,
 *        /auth/forgot-password, /auth/reset-password
 *   GET  /auth/verify-email
 *
 * Authenticated endpoints (JWT required):
 *   GET  /auth/me
 *   POST /auth/change-password, /auth/logout
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for registration, login, token management, and account security")
public class AuthController {

    private final AuthService authService;

    // ──────────────────────────────────────────────────────────
    // PUBLIC ENDPOINTS
    // ──────────────────────────────────────────────────────────

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register a new user",
            description = "Creates a new customer account and sends an email verification link. " +
                    "Password must be ≥8 chars with at least one uppercase letter and one number."
    )
    public ResponseEntity<ApiResponse<MessageResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        MessageResponse result = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", result));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate and obtain JWT tokens",
            description = "Returns an access token (24h) and refresh token (7 days) on successful authentication."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse result = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", result));
    }

    @PostMapping("/refresh-token")
    @Operation(
            summary = "Rotate JWT tokens",
            description = "Exchanges a valid refresh token for a new access + refresh token pair. " +
                    "The old refresh token is revoked immediately (rotation strategy)."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse result = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", result));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Initiate forgot password flow",
            description = "Sends a 15-minute password reset link to the registered email. " +
                    "Always returns success to prevent user enumeration."
    )
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        MessageResponse result = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Request processed", result));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password using token",
            description = "Completes the password reset flow. All existing sessions are revoked on success."
    )
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        MessageResponse result = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", result));
    }

    @GetMapping("/verify-email")
    @Operation(
            summary = "Verify email address",
            description = "Activates the user's email using the token from the verification email (24h validity)."
    )
    public ResponseEntity<ApiResponse<MessageResponse>> verifyEmail(
            @RequestParam("token") String token) {

        MessageResponse result = authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verification complete", result));
    }

    // ──────────────────────────────────────────────────────────
    // AUTHENTICATED ENDPOINTS (JWT required)
    // ──────────────────────────────────────────────────────────

    @GetMapping("/me")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Get current user profile",
            description = "Returns the profile of the currently authenticated user."
    )
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {
        UserProfileResponse result = authService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", result));
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Change password",
            description = "Updates the authenticated user's password. All sessions are revoked after change."
    )
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        MessageResponse result = authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed", result));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Logout — revoke refresh token",
            description = "Revokes the provided refresh token, invalidating that client session. " +
                    "The access token remains valid until expiry (client should discard it)."
    )
    public ResponseEntity<ApiResponse<MessageResponse>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {

        MessageResponse result = authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", result));
    }
}
