package com.example.memories.domain.auth.controller;

import com.example.memories.domain.auth.dto.response.LoginResponseDto;
import com.example.memories.domain.auth.service.AuthService;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.global.security.TokenCookieFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class OAuthCallbackControllerTest {

    @Mock AuthService authService;

    OAuthCallbackController controller;

    @BeforeEach
    void setUp() {
        TokenCookieFactory cookieFactory = new TokenCookieFactory(1800000L, 604800000L);
        controller = new OAuthCallbackController(authService, cookieFactory);

        ReflectionTestUtils.setField(controller, "frontendRedirectUri", "https://app.example.com/login/success");
    }

    @Test
    @DisplayName("콜백 시 토큰을 HttpOnly 쿠키로 내려주고 프론트로 리다이렉트한다")
    void callback_setsCookiesAndRedirects() {
        LoginResponseDto login = new LoginResponseDto(1L, "Test", "test@example.com", "access-token", "refresh-token");
        given(authService.login(AuthProvider.GOOGLE, "auth-code")).willReturn(login);

        ResponseEntity<Void> response = controller.callback("auth-code", AuthProvider.GOOGLE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("https://app.example.com/login/success");

        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).hasSize(2);
        assertThat(cookies).anySatisfy(c -> assertThat(c)
                .startsWith("accessToken=access-token")
                .contains("HttpOnly").contains("Secure").contains("SameSite=None"));
        assertThat(cookies).anySatisfy(c -> assertThat(c)
                .startsWith("refreshToken=refresh-token")
                .contains("HttpOnly").contains("Secure").contains("SameSite=None"));
    }
}
