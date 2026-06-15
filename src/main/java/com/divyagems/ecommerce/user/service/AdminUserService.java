package com.divyagems.ecommerce.user.service;

import com.divyagems.ecommerce.user.dto.response.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Admin-only user management service contract.
 * Provides paginated listing, individual lookup, and account toggle.
 */
public interface AdminUserService {

    /**
     * Return a paginated list of all users.
     * Optionally filter by free-text search (email or full name) and active status.
     *
     * @param search   optional free-text — matched against email and full name
     * @param isActive optional filter on active status (null = no filter)
     * @param pageable pagination and sort parameters
     */
    Page<UserProfileResponse> getAllUsers(String search, Boolean isActive, Pageable pageable);

    /**
     * Return the full profile for a specific user (admin access — no ownership check).
     *
     * @param userId the user's UUID
     */
    UserProfileResponse getUserById(UUID userId);

    /**
     * Toggle a user's isActive flag.
     * Deactivating a user revokes all their refresh tokens (force logout).
     *
     * @param userId the user's UUID
     * @return updated profile with new isActive state
     */
    UserProfileResponse toggleUserActive(UUID userId);
}
