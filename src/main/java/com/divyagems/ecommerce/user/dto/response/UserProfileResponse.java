package com.divyagems.ecommerce.user.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Full user profile response — richer than the auth-module's UserProfileResponse.
 * Includes alternatePhone, createdAt, and isActive (admin visibility).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String alternatePhone;
    private String profileImageUrl;
    private boolean isEmailVerified;
    private boolean isActive;
    private Set<String> roles;
    private LocalDateTime createdAt;
}
