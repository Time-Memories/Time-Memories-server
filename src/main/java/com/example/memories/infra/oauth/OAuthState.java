package com.example.memories.infra.oauth;

import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.global.exception.BusinessException;

/**
 * OAuth state 파라미터 값 객체.
 * provider 구분과 CSRF 방지용 nonce를 "provider:nonce" 형태로 함께 운반한다.
 */
public record OAuthState(AuthProvider provider, String nonce) {

    private static final String DELIMITER = ":";

    public String toParam() {
        return provider.name().toLowerCase() + DELIMITER + nonce;
    }

    public static OAuthState parse(String raw) {
        if (raw == null) {
            throw new BusinessException(AuthErrorCode.INVALID_OAUTH_STATE);
        }
        int idx = raw.indexOf(DELIMITER);
        if (idx <= 0 || idx == raw.length() - 1) {
            throw new BusinessException(AuthErrorCode.INVALID_OAUTH_STATE);
        }
        AuthProvider provider = parseProvider(raw.substring(0, idx));
        String nonce = raw.substring(idx + 1);
        return new OAuthState(provider, nonce);
    }

    private static AuthProvider parseProvider(String value) {
        try {
            return AuthProvider.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.UNSUPPORTED_PROVIDER);
        }
    }
}
