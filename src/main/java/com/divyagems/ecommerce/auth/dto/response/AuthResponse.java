package com.divyagems.ecommerce.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Access token lifetime in seconds (matches app.jwt.expiration-ms / 1000). */
    private long expiresIn;

    private UserProfileResponse user;
}
