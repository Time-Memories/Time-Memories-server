package com.example.memories.domain.auth.controller;

import com.example.memories.domain.auth.dto.response.LoginResponseDto;
import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.auth.service.AuthService;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.global.security.TokenCookieFactory;
import com.example.memories.infra.oauth.OAuthClient;
import com.example.memories.infra.oauth.OAuthClientComposite;
import com.example.memories.infra.oauth.OAuthState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@RestController
@RequiredArgsConstructor
@Tag(name = "OAuth", description = "소셜 로그인 API")
public class OAuthController {

    private static final String OAUTH_STATE_COOKIE = "oauthState";
    private static final String STATE_COOKIE_PATH = "/api/oauth";
    private static final Duration STATE_COOKIE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthService authService;
    private final OAuthClientComposite oAuthClientComposite;
    private final TokenCookieFactory tokenCookieFactory;

    @Value("${oauth.frontend-redirect-uri}")
    private String frontendRedirectUri;

    @Operation(
            summary = "소셜 로그인 시작",
            description = "CSRF 방지용 nonce를 생성해 쿠키에 저장하고, 제공자(구글/카카오) 로그인 페이지로 리다이렉트합니다. " +
                    "프론트엔드는 로그인 버튼에서 이 엔드포인트로 이동시키면 됩니다.")
    @GetMapping("/api/oauth/authorize/{provider}")
    public ResponseEntity<Void> authorize(@PathVariable AuthProvider provider,
                                          @RequestParam(name = "redirect_url", required = false) String redirectUrl) {
        String resolvedRedirectUri = resolveRedirectUri(redirectUrl);

        String nonce = generateNonce();
        OAuthState state = new OAuthState(provider, nonce, resolvedRedirectUri);

        OAuthClient client = oAuthClientComposite.getClient(provider);
        String authorizationUri = client.getAuthorizationUri(state.toParam());

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authorizationUri))
                .header(HttpHeaders.SET_COOKIE, stateCookie(nonce).toString())
                .build();
    }

    @Operation(
            summary = "소셜 로그인 콜백",
            description = "구글/카카오 로그인 후 리다이렉트되는 엔드포인트입니다. state의 nonce와 쿠키를 대조해 CSRF를 검증하고, " +
                    "인가 코드를 토큰으로 교환한 뒤 JWT를 HttpOnly 쿠키로 내려주고 프론트엔드로 리다이렉트합니다.")
    @GetMapping("/api/oauth")
    public ResponseEntity<Void> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            @CookieValue(name = OAUTH_STATE_COOKIE, required = false) String stateCookie) {

        OAuthState parsed = OAuthState.parse(state);
        if (stateCookie == null || !stateCookie.equals(parsed.nonce())) {
            throw new BusinessException(AuthErrorCode.INVALID_OAUTH_STATE);
        }

        LoginResponseDto login = authService.login(parsed.provider(), code);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(parsed.redirectUrl()))
                .header(HttpHeaders.SET_COOKIE, tokenCookieFactory.accessToken(login.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, tokenCookieFactory.refreshToken(login.refreshToken()).toString())
                .header(HttpHeaders.SET_COOKIE, expireStateCookie().toString())
                .build();
    }

    private String generateNonce() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ResponseCookie stateCookie(String nonce) {
        return baseStateCookie(nonce, STATE_COOKIE_TTL);
    }

    private ResponseCookie expireStateCookie() {
        return baseStateCookie("", Duration.ZERO);
    }

    private ResponseCookie baseStateCookie(String value, Duration maxAge) {
        return ResponseCookie.from(OAUTH_STATE_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(STATE_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    private String resolveRedirectUri(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return frontendRedirectUri;
        }

        if (redirectUrl.equals(frontendRedirectUri)
                || redirectUrl.equals("http://localhost:5173/oauth/callback")) {
            return redirectUrl;
        }

        throw new BusinessException(AuthErrorCode.INVALID_REDIRECT_URI);
    }
}
