package com.example.memories.global.websocket;

import com.example.memories.global.security.TokenCookieFactory;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@Component
public class JwtCookieHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ACCESS_TOKEN_ATTRIBUTE = "accessToken";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return true;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();

        Cookie[] cookies = httpRequest.getCookies();

        if (cookies == null) {
            log.warn("WebSocket Handshake - Cookie 없음");
            return true;
        }

        Arrays.stream(cookies)
                .filter(cookie -> TokenCookieFactory.ACCESS_TOKEN_COOKIE.equals(cookie.getName()))
                .findFirst()
                .ifPresent(cookie -> {
                    attributes.put(
                            ACCESS_TOKEN_ATTRIBUTE,
                            cookie.getValue()
                    );

                    log.debug("WebSocket AccessToken Cookie 저장");
                });

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }
}
