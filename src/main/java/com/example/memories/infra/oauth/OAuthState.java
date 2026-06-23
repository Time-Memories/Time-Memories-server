package com.example.memories.infra.oauth;

import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.global.exception.BusinessException;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth state 파라미터 값 객체.
 * provider 구분, CSRF 방지용 nonce, 프론트 redirectUrl을 "provider:nonce:encodedRedirectUrl" 형태로 함께 운반한다.
 */
public record OAuthState(AuthProvider provider, String nonce, String redirectUrl) {

    private static final String DELIMITER = ":";

    public String toParam() {
        return provider.name().toLowerCase()
                + DELIMITER
                + nonce
                + DELIMITER
                + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8);
    }

    public static OAuthState parse(String raw) {
        if (raw == null) {
            throw new BusinessException(AuthErrorCode.INVALID_OAUTH_STATE);
        }

        String[] parts = raw.split(DELIMITER, 3);

        if (parts.length != 3) {
            throw new BusinessException(AuthErrorCode.INVALID_OAUTH_STATE);
        }

        AuthProvider provider = parseProvider(parts[0]);
        String nonce = parts[1];
        String redirectUrl = URLDecoder.decode(parts[2], StandardCharsets.UTF_8);

        return new OAuthState(provider, nonce, redirectUrl);
    }

    private static AuthProvider parseProvider(String value) {
        try {
            return AuthProvider.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.UNSUPPORTED_PROVIDER);
        }
    }
}
