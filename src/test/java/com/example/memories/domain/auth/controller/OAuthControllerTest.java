package com.example.memories.domain.auth.controller;

import com.example.memories.domain.auth.dto.response.LoginResponseDto;
import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.auth.service.AuthService;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.global.security.TokenCookieFactory;
import com.example.memories.infra.oauth.OAuthClient;
import com.example.memories.infra.oauth.OAuthClientComposite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthControllerTest {

    @Mock AuthService authService;
    @Mock OAuthClientComposite oAuthClientComposite;
    @Mock OAuthClient oAuthClient;

    OAuthController controller;

    @BeforeEach
    void setUp() {
        TokenCookieFactory cookieFactory = new TokenCookieFactory(1800000L, 604800000L);
        controller = new OAuthController(authService, oAuthClientComposite, cookieFactory);
        ReflectionTestUtils.setField(controller, "frontendRedirectUri", "https://app.example.com/login/success");
    }

    @Test
    @DisplayName("authorize 시 nonce 쿠키를 심고 provider 로그인 URL로 리다이렉트한다")
    void authorize_setsStateCookieAndRedirects() {
        given(oAuthClientComposite.getClient(AuthProvider.GOOGLE)).willReturn(oAuthClient);
        given(oAuthClient.getAuthorizationUri(anyString())).willReturn("https://accounts.google.com/o/oauth2/v2/auth?state=google:n");

        ResponseEntity<Void> response = controller.authorize(AuthProvider.GOOGLE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("https://accounts.google.com/o/oauth2/v2/auth?state=google:n");

        // state(provider:nonce)와 쿠키의 nonce가 동일해야 한다
        ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
        then(oAuthClient).should().getAuthorizationUri(stateCaptor.capture());
        String state = stateCaptor.getValue();
        assertThat(state).startsWith("google:");
        String nonce = state.substring("google:".length());

        String stateCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(stateCookie)
                .startsWith("oauthState=" + nonce)
                .contains("HttpOnly").contains("Secure").contains("SameSite=Lax");
    }

    @Test
    @DisplayName("콜백 시 state nonce와 쿠키가 일치하면 로그인 후 토큰 쿠키를 내려준다")
    void callback_validState_setsTokenCookies() {
        LoginResponseDto login = new LoginResponseDto(1L, "Test", "test@example.com", "access-token", "refresh-token");
        given(authService.login(AuthProvider.GOOGLE, "auth-code")).willReturn(login);

        ResponseEntity<Void> response = controller.callback("auth-code", "google:nonce-123", "nonce-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("https://app.example.com/login/success");

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).anySatisfy(c -> assertThat(c).startsWith("accessToken=access-token"));
        assertThat(cookies).anySatisfy(c -> assertThat(c).startsWith("refreshToken=refresh-token"));
        // state 쿠키 만료
        assertThat(cookies).anySatisfy(c -> assertThat(c).startsWith("oauthState=").contains("Max-Age=0"));
    }

    @Test
    @DisplayName("콜백 시 state nonce와 쿠키가 다르면 INVALID_OAUTH_STATE 예외를 발생시킨다")
    void callback_stateMismatch_throws() {
        assertThatThrownBy(() -> controller.callback("auth-code", "google:nonce-123", "different-nonce"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_OAUTH_STATE));

        then(authService).should(never()).login(any(), any());
    }

    @Test
    @DisplayName("콜백 시 state 쿠키가 없으면 INVALID_OAUTH_STATE 예외를 발생시킨다")
    void callback_missingStateCookie_throws() {
        assertThatThrownBy(() -> controller.callback("auth-code", "google:nonce-123", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.INVALID_OAUTH_STATE));
    }
}
