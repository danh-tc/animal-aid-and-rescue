package com.aar.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String code, String message, Map<String, String> fieldErrors, OffsetDateTime timestamp) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Map.of(), OffsetDateTime.now());
    }

    public static ApiError of(String code, String message, Map<String, String> fieldErrors) {
        return new ApiError(code, message, fieldErrors, OffsetDateTime.now());
    }
}
