package com.example.memories.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequestDto(
        @NotBlank String token
) {}