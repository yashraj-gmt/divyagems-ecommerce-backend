package com.divyagems.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Divya Gems E-Commerce backend.
 * Step 4 completed
 * JPA Auditing (@EnableJpaAuditing) is configured separately in
 * {@link com.divyagems.ecommerce.config.AuditConfig} to keep the main
 * class clean and allow the auditorAwareRef bean to be properly wired.
 */
@SpringBootApplication
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }
}
