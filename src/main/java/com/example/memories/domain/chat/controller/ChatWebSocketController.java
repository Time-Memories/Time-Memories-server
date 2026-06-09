package com.example.memories.domain.chat.controller;

import com.example.memories.domain.chat.dto.request.ChatImageSendRequestDto;
import com.example.memories.domain.chat.dto.request.ChatSendRequestDto;
import com.example.memories.domain.chat.service.ChatService;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.annotation.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    /**
     * 텍스트 메시지 전송
     * SEND /app/rooms/{room_id}/chats
     */
    @MessageMapping("/rooms/{room_id}/chats")
    public void sendMessage(
            @DestinationVariable("room_id") Long roomId,
            @CurrentUser User user,
            @Payload @Valid ChatSendRequestDto request
    ) {
        log.info("채팅 전송 요청 - roomId={}, userId={}", roomId, user.getId());

        chatService.sendMessage(roomId, user, request);
    }

    /**
     * 이미지 메시지 전송
     * SEND /app/rooms/{room_id}/chats/images
     */
    @MessageMapping("/rooms/{room_id}/chats/images")
    public void sendImageMessage(
            @DestinationVariable("room_id") Long roomId,
            @CurrentUser User user,
            @Payload @Valid ChatImageSendRequestDto request
    ) {
        log.info("이미지 채팅 전송 요청 - roomId={}, userId={}, imageCount={}",
                roomId, user.getId(), request.imageKeys().size());

        chatService.sendImageMessage(roomId, user, request);
    }
}
