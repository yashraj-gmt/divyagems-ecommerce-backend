package com.divyagems.ecommerce.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Registers the local upload directory as a Spring MVC static resource location.
 *
 * Uploaded profile images are stored under:
 *   {uploadDir}/profile-images/{userId}/{uuid}.ext
 *
 * And are served publicly at:
 *   GET /uploads/profile-images/{userId}/{uuid}.ext
 *
 * The /uploads/** path is whitelisted in SecurityConfig so no JWT is required.
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath   = Paths.get(uploadDir).toAbsolutePath().normalize();
        String resourceLoc = uploadPath.toUri().toString();

        log.info("Serving static uploads from: {} → /uploads/**", resourceLoc);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLoc);
    }
}
