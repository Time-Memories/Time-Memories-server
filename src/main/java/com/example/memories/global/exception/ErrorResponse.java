package com.example.memories.global.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<String> errors,
        LocalDateTime timestamp,
        String path
) {
    public static ErrorResponse of (ErrorCode code, String message, List<String> errors, String path) {
        return new ErrorResponse(
                code.getCode(),
                message != null ? message : code.getMessage(),
                errors,
                LocalDateTime.now(),
                path
        );
    }
}
