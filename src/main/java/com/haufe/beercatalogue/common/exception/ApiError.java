package com.haufe.beercatalogue.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(description = "Error response body returned for all API error conditions")
public record ApiError(
    @Schema(description = "HTTP status code") int status,
    @Schema(description = "Human-readable error message") String message,
    @Schema(description = "UTC timestamp when the error occurred") Instant timestamp,
    @Schema(description = "Field-level validation errors; present only on 400 responses") Map<String, String> fieldErrors
) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, Instant.now(), null);
    }

    public static ApiError withFieldErrors(int status, String message, Map<String, String> fieldErrors) {
        return new ApiError(status, message, Instant.now(), fieldErrors);
    }
}
