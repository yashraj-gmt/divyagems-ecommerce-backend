package com.divyagems.ecommerce.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Standardized API response wrapper for all endpoints.
 * Null fields are omitted from JSON via @JsonInclude.
 *
 * @param <T> payload type
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private List<FieldErrorDetails> errors;
    private String timestamp;
    private String path;

    private StandardApiResponse(boolean success, String message, T data, List<FieldErrorDetails> errors) {
        this.success   = success;
        this.message   = message;
        this.data      = data;
        this.errors    = errors;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        try {
            this.path = ServletUriComponentsBuilder.fromCurrentRequest().build().getPath();
        } catch (Exception e) {
            this.path = "unknown";
        }
    }

    // ─── Factory Methods ───────────────────────────────────────

    public static <T> StandardApiResponse<T> success(T data) {
        return new StandardApiResponse<>(true, "Success", data, null);
    }

    public static <T> StandardApiResponse<T> success(String message, T data) {
        return new StandardApiResponse<>(true, message, data, null);
    }

    public static <T> StandardApiResponse<T> success(String message) {
        return new StandardApiResponse<>(true, message, null, null);
    }

    public static <T> StandardApiResponse<T> error(String message) {
        return new StandardApiResponse<>(false, message, null, null);
    }

    public static <T> StandardApiResponse<T> error(String message, List<FieldErrorDetails> errors) {
        return new StandardApiResponse<>(false, message, null, errors);
    }

    // ─── Nested FieldError ─────────────────────────────────────

    @Getter
    @NoArgsConstructor
    public static class FieldErrorDetails {
        private String field;
        private String message;

        public FieldErrorDetails(String field, String message) {
            this.field   = field;
            this.message = message;
        }

        public static FieldErrorDetails from(FieldError error) {
            return new FieldErrorDetails(error.getField(), error.getDefaultMessage());
        }
    }
}
