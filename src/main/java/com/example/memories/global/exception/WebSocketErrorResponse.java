package com.example.memories.global.exception;

public record WebSocketErrorResponse(
        String type,
        String message,
        String code
){
    public static WebSocketErrorResponse of(String type, String message, String code) {
        return new WebSocketErrorResponse(type, message, code);
    }
}
