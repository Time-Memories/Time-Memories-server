package com.example.memories.domain.chat.controller;

import com.example.memories.domain.chat.dto.response.ChatListResponseDto;
import com.example.memories.domain.chat.service.ChatService;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.annotation.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{room_id}/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "채팅 API")
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "채팅 기록 조회",
            description = "특정 방의 채팅 기록을 최신순으로 커서 기반 조회합니다. (기본 20개씩 불러옴)"
    )
    @GetMapping
    public ResponseEntity<ChatListResponseDto> getChats(
            @PathVariable("room_id") Long roomId,
            @CurrentUser User user,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {

        return ResponseEntity.ok(
                chatService.getChats(roomId, user, cursor, size)
        );
    }

    @Operation(
            summary = "채팅 삭제",
            description = "본인이 작성한 채팅 메시지를 삭제합니다."
    )
    @DeleteMapping("/{chat_id}")
    public ResponseEntity<Void> deleteChat(
            @PathVariable("room_id") Long roomId,
            @PathVariable("chat_id") Long chatId,
            @CurrentUser User user
    ) {

        chatService.deleteChat(roomId, chatId, user);

        return ResponseEntity.noContent().build();
    }
}
