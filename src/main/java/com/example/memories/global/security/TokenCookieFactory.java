package com.example.memories.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TokenCookieFactory {

    public static final String ACCESS_TOKEN_COOKIE = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public TokenCookieFactory(
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry) {
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public ResponseCookie accessToken(String value) {
        return build(ACCESS_TOKEN_COOKIE, value, Duration.ofMillis(accessTokenExpiry));
    }

    public ResponseCookie refreshToken(String value) {
        return build(REFRESH_TOKEN_COOKIE, value, Duration.ofMillis(refreshTokenExpiry));
    }

    public ResponseCookie expire(String name) {
        return build(name, "", Duration.ZERO);
    }

    private ResponseCookie build(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
