package com.divyagems.ecommerce.user.service;

import com.divyagems.ecommerce.auth.dto.response.MessageResponse;
import com.divyagems.ecommerce.user.dto.request.AddressRequest;
import com.divyagems.ecommerce.user.dto.request.ChangePasswordRequest;
import com.divyagems.ecommerce.user.dto.request.UpdateProfileRequest;
import com.divyagems.ecommerce.user.dto.response.AddressResponse;
import com.divyagems.ecommerce.user.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * User profile and address management service contract.
 * All methods operate on the currently authenticated user unless
 * explicitly delegated to admin operations.
 */
public interface ProfileService {

    // ─── Profile ──────────────────────────────────────────────

    /** Return the full profile of the currently authenticated user. */
    UserProfileResponse getMyProfile();

    /**
     * Apply non-null fields from the request onto the current user.
     * Email is immutable and cannot be changed.
     */
    UserProfileResponse updateMyProfile(UpdateProfileRequest request);

    /**
     * Store an uploaded profile image file to the configured upload directory,
     * update the user's profileImageUrl, and return the updated profile.
     *
     * @param file the multipart image file (jpg/png/webp/gif, max 10MB)
     */
    UserProfileResponse uploadProfileImage(MultipartFile file);

    /**
     * Change the current user's password after verifying the current one.
     * Revokes all active refresh tokens (forces re-login on all devices).
     */
    MessageResponse changePassword(ChangePasswordRequest request);

    // ─── Addresses ────────────────────────────────────────────

    /** Return all saved addresses for the current user, default address first. */
    List<AddressResponse> getMyAddresses();

    /**
     * Add a new address to the current user's address book.
     * If isDefault=true, all other addresses are un-defaulted first.
     */
    AddressResponse addAddress(AddressRequest request);

    /**
     * Update a specific address owned by the current user.
     * Throws 403 if the address belongs to a different user.
     */
    AddressResponse updateAddress(UUID id, AddressRequest request);

    /**
     * Delete a specific address owned by the current user.
     * Throws 400 if the address is referenced by an active (non-delivered/cancelled) order.
     */
    void deleteAddress(UUID id);

    /**
     * Set an address as the default, un-defaulting all others first.
     * Throws 403 if the address does not belong to the current user.
     */
    AddressResponse setDefaultAddress(UUID id);
}
