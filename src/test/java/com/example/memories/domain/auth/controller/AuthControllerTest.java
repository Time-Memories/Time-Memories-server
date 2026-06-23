package com.example.memories.domain.auth.controller;

import com.example.memories.domain.auth.dto.response.TokenResponseDto;
import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.auth.service.AuthService;
import com.example.memories.global.exception.BusinessException;
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

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock AuthService authService;

    AuthController controller;

    @BeforeEach
    void setUp() {
        TokenCookieFactory cookieFactory = new TokenCookieFactory(1800000L, 604800000L);
        controller = new AuthController(authService, cookieFactory);
    }

    @Test
    @DisplayName("refresh 쿠키로 재발급하면 새 토큰을 쿠키로 내려준다")
    void refresh_success_setsCookies() {
        given(authService.refresh("old-refresh")).willReturn(new TokenResponseDto("new-access", "new-refresh"));

        ResponseEntity<Void> response = controller.refresh("old-refresh");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).hasSize(2);
        assertThat(cookies).anySatisfy(c -> assertThat(c).startsWith("accessToken=new-access").contains("HttpOnly"));
        assertThat(cookies).anySatisfy(c -> assertThat(c).startsWith("refreshToken=new-refresh").contains("HttpOnly"));
    }

    @Test
    @DisplayName("refresh 쿠키가 없으면 REFRESH_TOKEN_NOT_FOUND 예외를 발생시킨다")
    void refresh_missingCookie_throws() {
        assertThatThrownBy(() -> controller.refresh(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        then(authService).should(never()).refresh(any());
    }

    @Test
    @DisplayName("로그아웃 시 토큰을 삭제하고 인증 쿠키를 만료시킨다")
    void logout_expiresCookies() {
        ResponseEntity<Void> response = controller.logout(1L);

        then(authService).should().logout(1L);
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).hasSize(2);
        assertThat(cookies).allSatisfy(c -> assertThat(c).contains("Max-Age=0"));
    }
}