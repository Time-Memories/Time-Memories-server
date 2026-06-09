package com.example.memories.global.websocket;

import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?>preSend(Message<?> message, MessageChannel channel) {
        // STOMP 메시지의 헤더 정보 접근 객체
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        // 최초 WebSocket 연결 (CONNECT)시 JWT 인증 수행
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("WebSocket CONNECT 요청");
            authenticateUser(accessor);
            return message;
        }

        // CONNECT 시 저장한 인증 정보를 이후 SEND, SUBSCRIBE 요청에서도 사용
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication) {

            // 현재 메시지 처리 스레드의 SecurityContext에 인증 정보 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // WebSocket 세션에 사용자 정보 저장 (이후 메시지에서 재사용)
            accessor.setUser(authentication);

            log.debug("WebSocket {} 요청 - userId={}",
                    accessor.getCommand(),
                    authentication.getPrincipal());
        }

        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            StompCommand command = accessor.getCommand();

            // 연결 종료(DISCONNECT) 시에만 SecurityContext 정리
            if (StompCommand.DISCONNECT.equals(command)) {
                SecurityContextHolder.clearContext();
                log.info("WebSocket DISCONNECT");
            }
        }
    }

    /**
     * CONNECT 요청의 Authorization 헤더에서 JWT를 추출하여 인증 처리
     */
    private void authenticateUser(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("WebSocket 인증 실패 - Authorization 헤더 없음");
            throw new BusinessException(AuthErrorCode.AUTHENTICATION_REQUIRED);
        }

        String token = authHeader.substring(7);

        // Access Token 검증 및 userId 추출
        Long userId = jwtProvider.extractUserIdFromAccessToken(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of()
                );

        // 현재 스레드 인증 정보 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // WebSocket 세션에 사용자 정보 저장 (이후 메시지에서 재사용)
        accessor.setUser(authentication);

        log.info("WebSocket 연결 인증 성공 - 사용자: {}", userId);
    }
}
