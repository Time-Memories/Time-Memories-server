package com.example.memories.domain.chat.dto.response;

public record ErrorResponseDto (
        String type,
        String message,
        String code
){
    public static ErrorResponseDto of(String type, String message, String code) {
        return new ErrorResponseDto(type, message, code);
    }
}
