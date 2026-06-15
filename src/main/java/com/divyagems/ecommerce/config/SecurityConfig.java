package com.divyagems.ecommerce.config;

import com.divyagems.ecommerce.security.jwt.JwtAuthenticationEntryPoint;
import com.divyagems.ecommerce.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the Divya Gems REST API.
 *
 * Key decisions:
 * - STATELESS session: JWT carries all authentication state; no HttpSession is created.
 * - CSRF disabled: safe for stateless REST APIs not using cookie-based auth.
 * - BCrypt(strength=12): strong password hashing with work factor tuned for prod.
 * - PasswordEncoder is declared here (separate from AuditConfig) to avoid
 *   circular bean dependency with AuthServiceImpl.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService userDetailsService;

    // ─── Public URL patterns ───────────────────────────────────
    private static final String[] PUBLIC_POST_PATHS = {
            "/auth/login",
            "/auth/register",
            "/auth/refresh-token",
            "/auth/forgot-password",
            "/auth/reset-password"
    };

    private static final String[] PUBLIC_GET_PATHS = {
            "/auth/verify-email",
            "/products/**",
            "/categories/**",
            "/tags/**",
            // Order tracking — public with phone verification
            "/orders/*/track",
            // Product reviews (public listing + summary)
            "/products/*/reviews",
            "/products/*/reviews/**",
            // Search endpoints (all public)
            "/search",
            "/search/**",
            // Uploaded static files (profile images, etc.)
            "/uploads/**",
            // Swagger / OpenAPI
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/v3/api-docs/**",
            // Actuator health (public)
            "/actuator/health"
    };

    /**
     * Cart endpoints are open to both guests and authenticated users.
     * Guest identity is maintained via the CART_SESSION_ID cookie.
     * /cart/merge is protected at the method level with @PreAuthorize("isAuthenticated()").
     */
    private static final String[] CART_PATHS = {
            "/cart",
            "/cart/items",
            "/cart/items/**",
            "/cart/validate",
            "/cart/merge"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── Stateless REST API ──
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── Authorization rules ──
                .authorizeHttpRequests(auth -> auth
                        // Public POST endpoints
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_PATHS).permitAll()
                        // Public GET endpoints
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()
                        // Cart: open to guests and users (merge guarded by @PreAuthorize)
                        .requestMatchers(CART_PATHS).permitAll()
                        // Admin-only area
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Everything else requires a valid JWT
                        .anyRequest().authenticated()
                )

                // ── 401 handler ──
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // ── Auth provider ──
                .authenticationProvider(daoAuthenticationProvider())

                // ── JWT filter ──
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Uses Spring Boot 3+ AuthenticationConfiguration to avoid circular beans.
     * AuthServiceImpl injects this to authenticate login credentials.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
