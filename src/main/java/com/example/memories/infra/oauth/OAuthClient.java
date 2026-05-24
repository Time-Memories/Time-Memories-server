package com.example.memories.infra.oauth;

import com.example.memories.domain.user.entity.AuthProvider;

public interface OAuthClient {
    OAuthUserInfo getUserInfo(String accessToken);
    AuthProvider getProvider();
}