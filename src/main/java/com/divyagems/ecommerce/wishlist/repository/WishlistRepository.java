package com.divyagems.ecommerce.wishlist.repository;

import com.divyagems.ecommerce.entity.Product;
import com.divyagems.ecommerce.entity.User;
import com.divyagems.ecommerce.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findByUserOrderByCreatedAtDesc(User user);

    boolean existsByUserAndProductAndVariantIsNull(User user, Product product);

    boolean existsByUserAndProduct_IdAndVariant_Id(User user, UUID productId, UUID variantId);

    boolean existsByUserAndProduct(User user, Product product);

    Optional<WishlistItem> findByIdAndUser(UUID id, User user);
}
