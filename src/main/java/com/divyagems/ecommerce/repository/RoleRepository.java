package com.divyagems.ecommerce.repository;

import com.divyagems.ecommerce.entity.Role;
import com.divyagems.ecommerce.enums.RoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleEnum name);
}
