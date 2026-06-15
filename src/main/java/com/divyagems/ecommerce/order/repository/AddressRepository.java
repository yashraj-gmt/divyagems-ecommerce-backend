package com.divyagems.ecommerce.order.repository;

import com.divyagems.ecommerce.entity.Address;
import com.divyagems.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    /**
     * Find a saved address by ID AND user — ensures users can only reference their own addresses.
     */
    Optional<Address> findByIdAndUser(UUID id, User user);
}
