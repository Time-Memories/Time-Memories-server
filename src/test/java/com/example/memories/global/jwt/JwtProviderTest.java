package com.example.memories.global.jwt;

import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.global.exception.BusinessException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-for-unit-test-min-32-chars!!";
    private static final Long USER_ID = 1L;

    private JwtProvider jwtProvider;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret", SECRET);
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpiry", 1800000L);
        ReflectionTestUtils.setField(jwtProvider, "refreshTokenExpiry", 604800000L);
        jwtProvider.init();
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Access Token 생성 후 userId를 정확히 추출한다")
    void extractUserIdFromAccessToken_success() {
        String token = jwtProvider.generateAccessToken(USER_ID);

        Long result = jwtProvider.extractUserIdFromAccessToken(token);

        assertThat(result).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Refresh Token 생성 후 userId를 정확히 추출한다")
    void extractUserIdFromRefreshToken_success() {
        String token = jwtProvider.generateRefreshToken(USER_ID);

        Long result = jwtProvider.extractUserIdFromRefreshToken(token);

        assertThat(result).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Refresh Token으로 Access 추출 메서드 호출 시 INVALID_TOKEN 예외 발생")
    void extractUserIdFromAccessToken_withRefreshToken_throwsInvalidToken() {
        String refreshToken = jwtProvider.generateRefreshToken(USER_ID);

        assertThatThrownBy(() -> jwtProvider.extractUserIdFromAccessToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("Access Token으로 Refresh 추출 메서드 호출 시 INVALID_TOKEN 예외 발생")
    void extractUserIdFromRefreshToken_withAccessToken_throwsInvalidToken() {
        String accessToken = jwtProvider.generateAccessToken(USER_ID);

        assertThatThrownBy(() -> jwtProvider.extractUserIdFromRefreshToken(accessToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("만료된 Access Token 검증 시 TOKEN_EXPIRED 예외 발생")
    void extractUserIdFromAccessToken_expired_throwsTokenExpired() {
        String expiredToken = buildExpiredToken("access");

        assertThatThrownBy(() -> jwtProvider.extractUserIdFromAccessToken(expiredToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.TOKEN_EXPIRED));
    }

    @Test
    @DisplayName("만료된 Refresh Token 검증 시 TOKEN_EXPIRED 예외 발생")
    void extractUserIdFromRefreshToken_expired_throwsTokenExpired() {
        String expiredToken = buildExpiredToken("refresh");

        assertThatThrownBy(() -> jwtProvider.extractUserIdFromRefreshToken(expiredToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.TOKEN_EXPIRED));
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 시 INVALID_TOKEN 예외 발생")
    void extractUserIdFromAccessToken_malformed_throwsInvalidToken() {
        assertThatThrownBy(() -> jwtProvider.extractUserIdFromAccessToken("not.a.jwt"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }

    private String buildExpiredToken(String type) {
        return Jwts.builder()
                .subject(String.valueOf(USER_ID))
                .claim("type", type)
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(secretKey)
                .compact();
    }
}
