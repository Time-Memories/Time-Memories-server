package com.example.memories.global.websocket;

import com.example.memories.domain.auth.exception.AuthErrorCode;
import com.example.memories.domain.room.exception.RoomErrorCode;
import com.example.memories.domain.room.repository.RoomUserRepository;
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
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final RoomUserRepository roomUserRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
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

            // SUBSCRIBE 요청 시 방 토픽 구독 권한 검증
            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                validateRoomSubscription(accessor, authentication);
            }

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

            // 연결 종료(DISCONNECT) 시 로그 출력
            if (StompCommand.DISCONNECT.equals(command)) {
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

        // WebSocket 세션에 사용자 정보 저장 (이후 메시지에서 재사용)
        accessor.setUser(authentication);

        log.info("WebSocket 연결 인증 성공 - 사용자: {}", userId);
    }

    /**
     * SUBSCRIBE 요청 시 해당 방의 멤버만 방 토픽을 구독할 수 있도록 검증
     */
    private void validateRoomSubscription(
            StompHeaderAccessor accessor,
            UsernamePasswordAuthenticationToken authentication
    ) {
        String destination = accessor.getDestination();

        if (destination == null) {
            return;
        }

        // 방 메시지/업데이트 토픽만 검증
        if (!destination.startsWith("/topic/rooms/")) {
            return;
        }

        Long userId = (Long) authentication.getPrincipal();
        Long roomId = extractRoomIdFromDestination(destination);
        if (!roomUserRepository.existsByRoomIdAndUserId(roomId, userId)) {
            log.warn("WebSocket 구독 권한 없음 - userId={}, roomId={}, destination={}",
                    userId, roomId, destination);
            throw new BusinessException(RoomErrorCode.ROOM_FORBIDDEN);
        }
    }

    /**
     * /topic/rooms/{roomId}, /topic/rooms/{roomId}/updates 에서 roomId 추출
     */
    private Long extractRoomIdFromDestination(String destination) {
        try {
            String path = destination.substring("/topic/rooms/".length());
            String roomId = path.split("/")[0];

            return Long.parseLong(roomId);
        } catch (RuntimeException e) {
            throw new BusinessException(RoomErrorCode.ROOM_FORBIDDEN);
        }
    }
}
