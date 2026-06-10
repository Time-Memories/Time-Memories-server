package com.example.memories.domain.auth.controller;

import com.example.memories.domain.auth.dto.response.LoginResponseDto;
import com.example.memories.domain.auth.service.AuthService;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.global.security.TokenCookieFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Tag(name = "OAuth", description = "소셜 로그인 콜백 API")
public class OAuthCallbackController {

    private final AuthService authService;
    private final TokenCookieFactory tokenCookieFactory;

    @Value("${oauth.frontend-redirect-uri}")
    private String frontendRedirectUri;

    @Operation(
            summary = "소셜 로그인 콜백",
            description = "구글/카카오 로그인 후 리다이렉트되는 엔드포인트입니다. " +
                    "state로 제공자를 구분하여 인가 코드를 토큰으로 교환하고, " +
                    "JWT를 HttpOnly 쿠키로 내려준 뒤 프론트엔드로 리다이렉트합니다.")
    @GetMapping("/api/oauth")
    public ResponseEntity<Void> callback(
            @RequestParam("code") String code,
            @RequestParam("state") AuthProvider provider) {

        LoginResponseDto login = authService.login(provider, code);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendRedirectUri))
                .header(HttpHeaders.SET_COOKIE, tokenCookieFactory.accessToken(login.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, tokenCookieFactory.refreshToken(login.refreshToken()).toString())
                .build();
    }
}
