package com.example.memories.domain.auth.controller;

import com.example.memories.domain.auth.dto.request.SocialLoginRequestDto;
import com.example.memories.domain.auth.dto.response.LoginResponseDto;
import com.example.memories.domain.auth.dto.response.TokenResponseDto;
import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.auth.service.AuthService;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.global.security.TokenCookieFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final AuthService authService;
    private final TokenCookieFactory tokenCookieFactory;

    @Operation(
            summary = "소셜 로그인/회원가입",
            description = "카카오 또는 구글 인가 코드(authorization code)로 로그인합니다. 서버가 코드를 토큰으로 교환하여 사용자 정보를 조회하며, 최초 로그인 시 자동 회원가입됩니다.(현재는 사용 X, 모바일 앱 개발 시 사용)"
    )
    @PostMapping("/login/{provider}")
    public ResponseEntity<LoginResponseDto> login(
            @PathVariable AuthProvider provider,
            @RequestBody @Valid SocialLoginRequestDto request) {

        return ResponseEntity.ok(authService.login(provider, request.code()));
    }

    @Operation(summary = "토큰 재발급", description = "HttpOnly 쿠키에 담긴 Refresh Token으로 새로운 Access/Refresh Token을 발급해 쿠키로 내려줍니다.")
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = TokenCookieFactory.REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        TokenResponseDto tokens = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenCookieFactory.accessToken(tokens.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, tokenCookieFactory.refreshToken(tokens.refreshToken()).toString())
                .build();
    }

    @Operation(summary = "로그아웃", description = "서버에 저장된 Refresh Token을 삭제하고 인증 쿠키를 제거합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Long userId) {
        authService.logout(userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenCookieFactory.expire(TokenCookieFactory.ACCESS_TOKEN_COOKIE).toString())
                .header(HttpHeaders.SET_COOKIE, tokenCookieFactory.expire(TokenCookieFactory.REFRESH_TOKEN_COOKIE).toString())
                .build();
    }
}