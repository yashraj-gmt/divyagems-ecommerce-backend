package com.divyagems.ecommerce.auth.service;

import com.divyagems.ecommerce.auth.dto.request.*;
import com.divyagems.ecommerce.auth.dto.response.AuthResponse;
import com.divyagems.ecommerce.auth.dto.response.MessageResponse;
import com.divyagems.ecommerce.auth.dto.response.UserProfileResponse;

public interface AuthService {

    /**
     * Register a new customer account.
     * Sends an email verification link async after successful registration.
     */
    MessageResponse register(RegisterRequest request);

    /**
     * Authenticate credentials and return a JWT access + refresh token pair.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Validate an existing refresh token and return a fresh token pair.
     * The old refresh token is revoked after use (rotation strategy).
     */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /**
     * Initiate the forgot-password flow.
     * Always returns success (even if email not found) to prevent user enumeration.
     */
    MessageResponse forgotPassword(ForgotPasswordRequest request);

    /**
     * Complete the password reset using the token received via email.
     * All existing refresh tokens for the user are revoked on success.
     */
    MessageResponse resetPassword(ResetPasswordRequest request);

    /**
     * Verify the user's email using the token from the verification email.
     */
    MessageResponse verifyEmail(String token);

    /**
     * Return the profile of the currently authenticated user.
     */
    UserProfileResponse getMyProfile();

    /**
     * Change the current user's password after validating the existing one.
     */
    MessageResponse changePassword(ChangePasswordRequest request);

    /**
     * Revoke the given refresh token, effectively logging out the client session.
     */
    MessageResponse logout(RefreshTokenRequest request);
}
