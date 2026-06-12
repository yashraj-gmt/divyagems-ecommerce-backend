package com.divyagems.ecommerce.entity;

import com.divyagems.ecommerce.enums.RoleEnum;
import jakarta.persistence.*;
import lombok.*;

/**
 * Role entity representing a user authority (e.g. ROLE_ADMIN, ROLE_USER).
 * Roles are referenced via many-to-many from the User entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private RoleEnum name;

    @Column(name = "description", length = 255)
    private String description;
}
