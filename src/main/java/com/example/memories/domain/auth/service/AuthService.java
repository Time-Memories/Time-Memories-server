package com.example.memories.domain.auth.service;

import com.example.memories.domain.auth.dto.response.LoginResponseDto;
import com.example.memories.domain.auth.dto.response.TokenResponseDto;
import com.example.memories.domain.user.entity.AuthProvider;

public interface AuthService {
    LoginResponseDto login(AuthProvider provider, String token);
    TokenResponseDto refresh(String refreshTokenValue);
    void logout(Long userId);
}