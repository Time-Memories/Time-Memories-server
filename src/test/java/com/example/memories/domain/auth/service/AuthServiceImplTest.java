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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock OAuthClientComposite oAuthClientComposite;
    @Mock UserService userService;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtProvider jwtProvider;
    @Mock OAuthClient oAuthClient;

    @InjectMocks AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiry", 604800000L);
    }

    @Test
    @DisplayName("소셜 로그인 성공 시 사용자 정보와 토큰을 반환한다")
    void login_success() {
        User user = User.builder()
                .name("Test User").email("test@example.com")
                .provider(AuthProvider.GOOGLE).providerId("google-id").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        OAuthUserInfo userInfo = new OAuthUserInfo("google-id", "test@example.com", "Test User", AuthProvider.GOOGLE);

        given(oAuthClientComposite.getClient(AuthProvider.GOOGLE)).willReturn(oAuthClient);
        given(oAuthClient.getUserInfo("social-token")).willReturn(userInfo);
        given(userService.findOrRegisterOAuthUser(AuthProvider.GOOGLE, "google-id", "Test User", "test@example.com")).willReturn(user);
        given(jwtProvider.generateAccessToken(1L)).willReturn("access-token");
        given(jwtProvider.generateRefreshToken(1L)).willReturn("refresh-token");

        LoginResponseDto result = authService.login(AuthProvider.GOOGLE, "social-token");

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        then(refreshTokenRepository).should().save(eq(1L), eq("refresh-token"), any());
    }

    @Test
    @DisplayName("유효한 Refresh Token으로 새로운 토큰을 발급한다")
    void refresh_success() {
        String oldRefreshToken = "old-refresh-token";
        given(jwtProvider.extractUserIdFromRefreshToken(oldRefreshToken)).willReturn(1L);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of(oldRefreshToken));
        given(jwtProvider.generateAccessToken(1L)).willReturn("new-access-token");
        given(jwtProvider.generateRefreshToken(1L)).willReturn("new-refresh-token");

        TokenResponseDto result = authService.refresh(oldRefreshToken);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        then(refreshTokenRepository).should().save(eq(1L), eq("new-refresh-token"), any());
    }

    @Test
    @DisplayName("저장된 토큰과 다른 값이 들어오면 REFRESH_TOKEN_REUSED 예외를 발생시키고 토큰을 삭제한다")
    void refresh_tokenMismatch_throwsAndDeletes() {
        given(jwtProvider.extractUserIdFromRefreshToken("reused-token")).willReturn(1L);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.of("stored-token"));

        assertThatThrownBy(() -> authService.refresh("reused-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.REFRESH_TOKEN_REUSED));
        then(refreshTokenRepository).should().delete(1L);
    }

    @Test
    @DisplayName("Redis에 저장된 Refresh Token이 없으면 REFRESH_TOKEN_REUSED 예외를 발생시킨다")
    void refresh_tokenNotStored_throwsRefreshTokenReused() {
        given(jwtProvider.extractUserIdFromRefreshToken("some-token")).willReturn(1L);
        given(refreshTokenRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("some-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.REFRESH_TOKEN_REUSED));
    }

    @Test
    @DisplayName("로그아웃 시 저장된 Refresh Token을 삭제한다")
    void logout_deletesRefreshToken() {
        authService.logout(1L);

        then(refreshTokenRepository).should().delete(1L);
    }
}
