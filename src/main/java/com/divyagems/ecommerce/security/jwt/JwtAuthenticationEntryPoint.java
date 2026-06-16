package com.divyagems.ecommerce.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles requests that arrive without a valid JWT.
 * Returns a structured 401 JSON response instead of the default HTML error page.
 *
 * Response body:
 * {
 *   "error": "Unauthorized",
 *   "message": "<exception message>",
 *   "path": "<request URI>",
 *   "timestamp": "<ISO timestamp>"
 * }
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("Unauthorized request to {}: {}", request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String message = authException.getMessage() != null
                ? authException.getMessage()
                : "Full authentication is required to access this resource";
        
        // Escape quotes to be safe
        message = message.replace("\"", "\\\"");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String path = request.getRequestURI();

        String jsonResponse = String.format(
                "{\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\",\"timestamp\":\"%s\"}",
                message, path, timestamp
        );

        response.getWriter().write(jsonResponse);
    }
}
