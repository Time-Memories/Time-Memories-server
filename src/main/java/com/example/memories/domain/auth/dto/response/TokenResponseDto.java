package com.example.memories.domain.auth.dto.response;

public record TokenResponseDto(
        String accessToken,
        String refreshToken
) {}