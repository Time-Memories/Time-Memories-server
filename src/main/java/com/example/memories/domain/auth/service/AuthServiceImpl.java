package com.example.memories.domain.auth.service;

import com.example.memories.domain.auth.dto.response.LoginResponseDto;
import com.example.memories.domain.auth.dto.response.TokenResponseDto;
import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.auth.repository.RefreshTokenRepository;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.domain.user.entity.User;
import com.example.memories.domain.user.service.UserService;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.global.jwt.JwtProvider;
import com.example.memories.infra.oauth.OAuthClient;
import com.example.memories.infra.oauth.OAuthClientComposite;
import com.example.memories.infra.oauth.OAuthUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final OAuthClientComposite oAuthClientComposite;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @Override
    public LoginResponseDto login(AuthProvider provider, String token) {
        OAuthClient client = oAuthClientComposite.getClient(provider);
        OAuthUserInfo userInfo = client.getUserInfo(token);

        User user = userService.findOrRegisterOAuthUser(
                userInfo.provider(), userInfo.providerId(), userInfo.name(), userInfo.email());

        String accessToken = jwtProvider.generateAccessToken(user.getId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken, expiresAt());

        return new LoginResponseDto(user.getId(), user.getName(), user.getEmail(), accessToken, refreshToken);
    }

    @Override
    public TokenResponseDto refresh(String refreshTokenValue) {
        Long userId = jwtProvider.extractUserIdFromRefreshToken(refreshTokenValue);

        refreshTokenRepository.findByUserId(userId)
                .filter(stored -> stored.equals(refreshTokenValue))
                .orElseThrow(() -> {
                    refreshTokenRepository.delete(userId);
                    return new BusinessException(AuthErrorCode.REFRESH_TOKEN_REUSED);
                });

        String newAccessToken = jwtProvider.generateAccessToken(userId);
        String newRefreshToken = jwtProvider.generateRefreshToken(userId);
        refreshTokenRepository.save(userId, newRefreshToken, expiresAt());

        return new TokenResponseDto(newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(Long userId) {
        refreshTokenRepository.delete(userId);
    }

    private LocalDateTime expiresAt() {
        return LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiry));
    }
}
