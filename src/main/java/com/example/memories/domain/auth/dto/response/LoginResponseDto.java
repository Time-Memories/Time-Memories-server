package com.example.memories.domain.auth.dto.response;

public record LoginResponseDto(
        Long userId,
        String name,
        String email,
        String accessToken,
        String refreshToken
) {}