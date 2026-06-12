package com.divyagems.ecommerce.auth.service.impl;

import com.divyagems.ecommerce.auth.dto.request.*;
import com.divyagems.ecommerce.auth.dto.response.AuthResponse;
import com.divyagems.ecommerce.auth.dto.response.MessageResponse;
import com.divyagems.ecommerce.auth.dto.response.UserProfileResponse;
import com.divyagems.ecommerce.auth.service.AuthService;
import com.divyagems.ecommerce.email.EmailService;
import com.divyagems.ecommerce.entity.RefreshToken;
import com.divyagems.ecommerce.entity.Role;
import com.divyagems.ecommerce.entity.User;
import com.divyagems.ecommerce.enums.RoleEnum;
import com.divyagems.ecommerce.repository.RefreshTokenRepository;
import com.divyagems.ecommerce.repository.RoleRepository;
import com.divyagems.ecommerce.repository.UserRepository;
import com.divyagems.ecommerce.security.UserDetailsImpl;
import com.divyagems.ecommerce.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ─── Register ──────────────────────────────────────────────

    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        // 1. Email uniqueness check
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new IllegalArgumentException("Email address is already registered: " + request.getEmail());
        }

        // 2. Resolve default role
        Role userRole = roleRepository.findByName(RoleEnum.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_USER not found in database. Ensure seed data is loaded via Flyway."));

        // 3. Generate email verification token (UUID, 24h expiry)
        String verificationToken = UUID.randomUUID().toString();
        LocalDateTime verificationExpiry = LocalDateTime.now().plusHours(24);

        // 4. Build and persist the user
        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .roles(Set.of(userRole))
                .emailVerificationToken(verificationToken)
                .emailVerificationExpiry(verificationExpiry)
                .isEmailVerified(false)
                .isActive(true)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // 5. Send verification email asynchronously (won't block response)
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        return new MessageResponse(
                "Registration successful! Please check your email to verify your account.");
    }

    // ─── Login ─────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate credentials via Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 2. Generate tokens
        String accessToken = jwtUtils.generateAccessToken(userDetails);
        String rawRefreshToken = jwtUtils.generateRefreshToken();

        // 3. Persist refresh token
        User user = userRepository.findByEmail(userDetails.getEmail())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB"));

        RefreshToken refreshToken = RefreshToken.builder()
                .token(rawRefreshToken)
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresIn(jwtUtils.getExpirationInSeconds())
                .user(mapToUserProfile(user))
                .build();
    }

    // ─── Refresh Token ─────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found or already used"));

        // Validate token
        if (storedToken.isRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked. Please log in again.");
        }
        if (storedToken.getExpiryDate().isBefore(Instant.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new IllegalArgumentException("Refresh token has expired. Please log in again.");
        }

        // Revoke old token (rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Issue new token pair
        User user = storedToken.getUser();
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        String newAccessToken = jwtUtils.generateAccessToken(userDetails);
        String newRawRefreshToken = jwtUtils.generateRefreshToken();

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(newRawRefreshToken)
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        log.info("Token refreshed for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .expiresIn(jwtUtils.getExpirationInSeconds())
                .user(mapToUserProfile(user))
                .build();
    }

    // ─── Forgot Password ───────────────────────────────────────

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        // Silently succeed even if email not found — prevents user enumeration
        userRepository.findByEmail(request.getEmail().toLowerCase()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            LocalDateTime resetExpiry = LocalDateTime.now().plusMinutes(15);

            user.setPasswordResetToken(resetToken);
            user.setPasswordResetExpiry(resetExpiry);
            userRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
            log.info("Password reset initiated for: {}", user.getEmail());
        });

        return new MessageResponse(
                "If your email is registered, you will receive a password reset link shortly.");
    }

    // ─── Reset Password ────────────────────────────────────────

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        // 1. Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        // 2. Find user by reset token
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token"));

        // 3. Check token expiry
        if (user.getPasswordResetExpiry() == null ||
                user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token has expired. Please request a new one.");
        }

        // 4. Update password and clear token fields
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);

        // 5. Revoke all refresh tokens (force re-login on all devices)
        refreshTokenRepository.revokeAllByUser(user);

        log.info("Password reset successfully for: {}", user.getEmail());
        return new MessageResponse("Password has been reset successfully. Please log in with your new password.");
    }

    // ─── Verify Email ──────────────────────────────────────────

    @Override
    @Transactional
    public MessageResponse verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email verification token"));

        if (user.isEmailVerified()) {
            return new MessageResponse("Email is already verified.");
        }

        if (user.getEmailVerificationExpiry() == null ||
                user.getEmailVerificationExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Email verification token has expired. Please request a new verification email.");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);
        userRepository.save(user);

        log.info("Email verified for: {}", user.getEmail());
        return new MessageResponse("Email verified successfully! You can now log in.");
    }

    // ─── Get My Profile ────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
        return mapToUserProfile(user);
    }

    // ─── Change Password ───────────────────────────────────────

    @Override
    @Transactional
    public MessageResponse changePassword(ChangePasswordRequest request) {
        // 1. Validate new passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        // 2. Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // 3. Prevent reusing the same password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all other sessions (optional: keeps current session alive)
        refreshTokenRepository.revokeAllByUser(user);

        log.info("Password changed for: {}", user.getEmail());
        return new MessageResponse("Password changed successfully.");
    }

    // ─── Logout ────────────────────────────────────────────────

    @Override
    @Transactional
    public MessageResponse logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    log.info("Refresh token revoked for logout: {}", token.getUser().getEmail());
                });

        return new MessageResponse("Logged out successfully.");
    }

    // ─── Private Helpers ───────────────────────────────────────

    private UserProfileResponse mapToUserProfile(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImageUrl(user.getProfileImageUrl())
                .roles(roleNames)
                .isEmailVerified(user.isEmailVerified())
                .build();
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
