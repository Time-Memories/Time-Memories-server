package com.example.memories.infra.oauth;

import com.example.memories.domain.user.entity.AuthProvider;

public record OAuthUserInfo(
        String providerId,
        String email,
        String name,
        AuthProvider provider
) {}