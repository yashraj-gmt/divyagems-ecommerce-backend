package com.divyagems.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing configuration for Divya Gems.
 *
 * Enables automatic population of @CreatedBy and @LastModifiedBy fields
 * in all entities extending BaseEntity.
 *
 * The AuditorAware bean resolves the current actor from the Spring Security
 * context. Falls back to "SYSTEM" for background/anonymous operations.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication.getPrincipal().equals("anonymousUser")) {
                return Optional.of("SYSTEM");
            }

            // Returns the email (principal name) of the authenticated user
            return Optional.ofNullable(authentication.getName());
        };
    }
}
