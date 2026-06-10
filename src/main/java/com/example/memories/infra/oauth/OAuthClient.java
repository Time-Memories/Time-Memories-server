package com.example.memories.infra.oauth;

import com.example.memories.domain.user.entity.AuthProvider;

public interface OAuthClient {
    String getAccessToken(String authorizationCode);
    OAuthUserInfo getUserInfo(String accessToken);
    AuthProvider getProvider();
}