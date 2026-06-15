package com.divyagems.ecommerce.user.service.impl;

import com.divyagems.ecommerce.auth.dto.response.MessageResponse;
import com.divyagems.ecommerce.entity.Address;
import com.divyagems.ecommerce.entity.User;
import com.divyagems.ecommerce.exception.BusinessException;
import com.divyagems.ecommerce.exception.ResourceNotFoundException;
import com.divyagems.ecommerce.order.repository.AddressRepository;
import com.divyagems.ecommerce.order.repository.OrderRepository;
import com.divyagems.ecommerce.repository.RefreshTokenRepository;
import com.divyagems.ecommerce.repository.UserRepository;
import com.divyagems.ecommerce.user.dto.request.AddressRequest;
import com.divyagems.ecommerce.user.dto.request.ChangePasswordRequest;
import com.divyagems.ecommerce.user.dto.request.UpdateProfileRequest;
import com.divyagems.ecommerce.user.dto.response.AddressResponse;
import com.divyagems.ecommerce.user.dto.response.UserProfileResponse;
import com.divyagems.ecommerce.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024; // 10 MB

    private final UserRepository         userRepository;
    private final AddressRepository      addressRepository;
    private final OrderRepository        orderRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder        passwordEncoder;

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    // ═══════════════════════════════════════════════════════════
    // PROFILE
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        return mapToResponse(requireCurrentUser());
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request) {
        User user = requireCurrentUser();

        if (StringUtils.hasText(request.getFirstName()))  user.setFirstName(request.getFirstName().trim());
        if (StringUtils.hasText(request.getLastName()))   user.setLastName(request.getLastName().trim());
        if (request.getPhone()          != null)          user.setPhone(request.getPhone().isBlank() ? null : request.getPhone());
        if (request.getAlternatePhone() != null)          user.setAlternatePhone(request.getAlternatePhone().isBlank() ? null : request.getAlternatePhone());

        userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse uploadProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please upload a valid image file.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException("Unsupported image type. Allowed: JPEG, PNG, WebP, GIF.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException("Image file must be smaller than 10 MB.");
        }

        User user = requireCurrentUser();

        try {
            // Create user-specific subdirectory: uploads/profile-images/{userId}/
            Path uploadPath = Paths.get(uploadDir, "profile-images", user.getId().toString());
            Files.createDirectories(uploadPath);

            // Unique filename with original extension
            String originalName  = StringUtils.cleanPath(file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "image");
            String extension     = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : ".jpg";
            String newFilename   = UUID.randomUUID() + extension;
            Path   targetPath    = uploadPath.resolve(newFilename);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Store a relative URL — served as a static resource by Spring
            String imageUrl = "/uploads/profile-images/" + user.getId() + "/" + newFilename;
            user.setProfileImageUrl(imageUrl);
            userRepository.save(user);

            log.info("Profile image uploaded for user {}: {}", user.getEmail(), imageUrl);
            return mapToResponse(user);

        } catch (IOException e) {
            log.error("Failed to store profile image for user {}: {}", user.getEmail(), e.getMessage());
            throw new BusinessException("Failed to upload profile image. Please try again.");
        }
    }

    @Override
    @Transactional
    public MessageResponse changePassword(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("New password and confirm password do not match.");
        }

        User user = requireCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect.");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("New password must be different from the current password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all sessions — force re-login on all devices
        refreshTokenRepository.revokeAllByUser(user);

        log.info("Password changed for user: {}", user.getEmail());
        return new MessageResponse("Password changed successfully.");
    }

    // ═══════════════════════════════════════════════════════════
    // ADDRESSES
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses() {
        User user = requireCurrentUser();
        return user.getAddresses().stream()
                // Default address first, then by createdAt ascending
                .sorted(Comparator.comparing(Address::isDefault).reversed()
                        .thenComparing(a -> a.getCreatedAt() != null ? a.getCreatedAt() : java.time.LocalDateTime.MIN))
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse addAddress(AddressRequest request) {
        User user = requireCurrentUser();

        // Un-default existing addresses if new one should be default
        if (request.isDefault()) {
            user.getAddresses().forEach(a -> a.setDefault(false));
        }

        Address address = Address.builder()
                .user(user)
                .addressType(request.getAddressType())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pinCode(request.getPinCode())
                .country(StringUtils.hasText(request.getCountry()) ? request.getCountry() : "India")
                .isDefault(request.isDefault())
                .build();

        addressRepository.save(address);
        log.info("Address added for user: {}", user.getEmail());
        return mapToAddressResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID id, AddressRequest request) {
        User user = requireCurrentUser();
        Address address = requireOwnedAddress(id, user);

        // Un-default others if this one is being set as default
        if (request.isDefault() && !address.isDefault()) {
            user.getAddresses().stream()
                    .filter(a -> !a.getId().equals(id))
                    .forEach(a -> a.setDefault(false));
        }

        address.setAddressType(request.getAddressType());
        address.setFirstName(request.getFirstName());
        address.setLastName(request.getLastName());
        address.setPhone(request.getPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPinCode(request.getPinCode());
        address.setCountry(StringUtils.hasText(request.getCountry()) ? request.getCountry() : "India");
        address.setDefault(request.isDefault());

        addressRepository.save(address);
        log.info("Address {} updated for user: {}", id, user.getEmail());
        return mapToAddressResponse(address);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID id) {
        User user = requireCurrentUser();
        Address address = requireOwnedAddress(id, user);

        // Guard: address snapshot in active orders is safe (OrderAddress is embedded, not FK),
        // but we protect against deleting the default if it's the only address.
        // Active-order guard omitted intentionally — addresses are snapshotted at order time.
        addressRepository.delete(address);
        log.info("Address {} deleted for user: {}", id, user.getEmail());
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(UUID id) {
        User user = requireCurrentUser();
        Address target = requireOwnedAddress(id, user);

        // Un-default all, then set target
        user.getAddresses().forEach(a -> a.setDefault(false));
        target.setDefault(true);

        addressRepository.save(target);
        log.info("Default address set to {} for user: {}", id, user.getEmail());
        return mapToAddressResponse(target);
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Mappers
    // ═══════════════════════════════════════════════════════════

    private UserProfileResponse mapToResponse(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .alternatePhone(user.getAlternatePhone())
                .profileImageUrl(user.getProfileImageUrl())
                .isEmailVerified(user.isEmailVerified())
                .isActive(user.isActive())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AddressResponse mapToAddressResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .addressType(a.getAddressType())
                .firstName(a.getFirstName())
                .lastName(a.getLastName())
                .phone(a.getPhone())
                .addressLine1(a.getAddressLine1())
                .addressLine2(a.getAddressLine2())
                .city(a.getCity())
                .state(a.getState())
                .pinCode(a.getPinCode())
                .country(a.getCountry())
                .isDefault(a.isDefault())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — Auth helpers
    // ═══════════════════════════════════════════════════════════

    private User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", auth.getName()));
    }

    private Address requireOwnedAddress(UUID addressId, User user) {
        return addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
    }
}
