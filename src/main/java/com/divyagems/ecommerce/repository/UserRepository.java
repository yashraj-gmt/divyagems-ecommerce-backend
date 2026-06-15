package com.divyagems.ecommerce.repository;

import com.divyagems.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);

    @Modifying
    @Query("UPDATE User u SET u.passwordResetToken = null, u.passwordResetExpiry = null WHERE u.id = :id")
    void clearPasswordResetToken(@Param("id") UUID id);

    /** Count of users registered after a given timestamp — used for dashboard new-users-this-month stat. */
    long countByCreatedAtAfter(LocalDateTime since);
}
