package com.example.memories.domain.chat.controller;

import com.example.memories.domain.chat.dto.response.ChatListResponseDto;
import com.example.memories.domain.chat.dto.response.ChatResponseDto;
import com.example.memories.domain.chat.dto.ChatType;
import com.example.memories.domain.chat.service.ChatService;
import com.example.memories.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    @Test
    @DisplayName("채팅 기록 조회")
    void getChats() {
        // given
        Long roomId = 1L;
        Long cursor = null;
        int size = 20;
        User user = mock(User.class);

        ChatResponseDto chat = new ChatResponseDto(
                1L,
                1L,
                "홍길동",
                ChatType.TEXT,
                "안녕하세요",
                List.of(),
                LocalDateTime.now()
        );

        ChatListResponseDto responseDto = new ChatListResponseDto(
                List.of(chat),
                false,
                null
        );

        given(chatService.getChats(roomId, user, cursor, size))
                .willReturn(responseDto);

        // when
        ResponseEntity<ChatListResponseDto> response =
                chatController.getChats(roomId, user, cursor, size);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseDto);
        verify(chatService).getChats(roomId, user, cursor, size);
    }

    @Test
    @DisplayName("채팅 삭제")
    void deleteChat() {
        // given
        Long roomId = 1L;
        Long chatId = 1L;
        User user = mock(User.class);

        willDoNothing().given(chatService)
                .deleteChat(roomId, chatId, user);

        // when
        ResponseEntity<Void> response =
                chatController.deleteChat(roomId, chatId, user);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(chatService).deleteChat(roomId, chatId, user);
    }
}