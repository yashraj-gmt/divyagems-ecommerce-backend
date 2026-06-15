package com.divyagems.ecommerce.user.service.impl;

import com.divyagems.ecommerce.entity.User;
import com.divyagems.ecommerce.exception.ResourceNotFoundException;
import com.divyagems.ecommerce.repository.RefreshTokenRepository;
import com.divyagems.ecommerce.repository.UserRepository;
import com.divyagems.ecommerce.user.dto.response.UserProfileResponse;
import com.divyagems.ecommerce.user.service.AdminUserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // ─── Get All Users ────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(String search, Boolean isActive, Pageable pageable) {
        Specification<User> spec = buildUserSpec(search, isActive);
        return userRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    // ─── Get User By ID ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToResponse(user);
    }

    // ─── Toggle Active ────────────────────────────────────────

    @Override
    @Transactional
    public UserProfileResponse toggleUserActive(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean newActiveState = !user.isActive();
        user.setActive(newActiveState);
        userRepository.save(user);

        // Revoke all sessions when deactivating so they can't keep using old tokens
        if (!newActiveState) {
            refreshTokenRepository.revokeAllByUser(user);
            log.info("User {} deactivated — all sessions revoked", user.getEmail());
        } else {
            log.info("User {} re-activated", user.getEmail());
        }

        return mapToResponse(user);
    }

    // ─── Private Helpers ──────────────────────────────────────

    private Specification<User> buildUserSpec(String search, Boolean isActive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern)
                ));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

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
}
