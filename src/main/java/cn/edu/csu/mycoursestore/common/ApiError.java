package cn.edu.csu.mycoursestore.common;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        Map<String, String> fieldErrors
) {

    public static ApiError of(HttpStatus status, String code, String message) {
        return new ApiError(Instant.now(), status.value(), code, message, null);
    }

    public static ApiError of(HttpStatus status, String code, String message, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status.value(), code, message, fieldErrors);
    }
}