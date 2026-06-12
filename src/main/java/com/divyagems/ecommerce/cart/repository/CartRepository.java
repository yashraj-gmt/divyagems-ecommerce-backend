package com.divyagems.ecommerce.cart.repository;

import com.divyagems.ecommerce.entity.Cart;
import com.divyagems.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    /** Find the single active cart for an authenticated user. */
    Optional<Cart> findByUserAndIsActiveTrue(User user);

    /** Find the single active cart for a guest session. */
    Optional<Cart> findBySessionIdAndIsActiveTrue(String sessionId);

    /** Deactivate all carts for a user (used before assigning a new one). */
    @Modifying
    @Query("UPDATE Cart c SET c.isActive = false WHERE c.user = :user AND c.isActive = true")
    void deactivateAllForUser(@Param("user") User user);

    /** Check whether an active cart exists for the given session. */
    boolean existsBySessionIdAndIsActiveTrue(String sessionId);
}
