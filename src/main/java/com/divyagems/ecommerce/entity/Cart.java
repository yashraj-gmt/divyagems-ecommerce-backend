package com.divyagems.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Shopping cart entity.
 * Supports both authenticated users (user field) and guests (sessionId).
 * Only one isActive=true cart should exist per user — enforced at service layer.
 * CartItems are cascade-deleted when the cart is deleted (orphanRemoval=true).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "carts", indexes = {
        @Index(name = "idx_cart_session_id", columnList = "session_id"),
        @Index(name = "idx_cart_user_id", columnList = "user_id")
})
public class Cart extends BaseEntity {

    // Null for guest carts
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Guest session identifier
    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @OneToMany(mappedBy = "cart", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();
}
