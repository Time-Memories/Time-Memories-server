package com.example.memories.domain.auth.controller;

import com.example.memories.domain.auth.dto.request.RefreshTokenRequestDto;
import com.example.memories.domain.auth.dto.request.SocialLoginRequestDto;
import com.example.memories.domain.auth.dto.response.LoginResponseDto;
import com.example.memories.domain.auth.dto.response.TokenResponseDto;
import com.example.memories.domain.auth.service.AuthService;
import com.example.memories.domain.user.entity.AuthProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "소셜 로그인/회원가입", description = "카카오 또는 구글 Access Token으로 로그인합니다. 최초 로그인 시 자동 회원가입됩니다.")
    @PostMapping("/login/{provider}")
    public ResponseEntity<LoginResponseDto> login(
            @PathVariable AuthProvider provider,
            @RequestBody @Valid SocialLoginRequestDto request) {
        return ResponseEntity.ok(authService.login(provider, request.token()));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새로운 Access/Refresh Token을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(@RequestBody @Valid RefreshTokenRequestDto request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "로그아웃", description = "서버에 저장된 Refresh Token을 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Long userId) {
        authService.logout(userId);
        return ResponseEntity.ok().build();
    }
}