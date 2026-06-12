package com.divyagems.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Stores JWT refresh tokens for stateful token revocation.
 * Each token is linked to a user and has an expiry date.
 * Revoked tokens are kept for audit purposes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_token", columnList = "token")
})
public class RefreshToken extends BaseEntity {

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;
}
