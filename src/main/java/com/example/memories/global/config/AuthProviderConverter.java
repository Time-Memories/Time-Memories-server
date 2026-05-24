package com.example.memories.global.config;

import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.global.exception.BusinessException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AuthProviderConverter implements Converter<String, AuthProvider> {

    @Override
    public AuthProvider convert(String source) {
        try {
            return AuthProvider.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.UNSUPPORTED_PROVIDER);
        }
    }
}
