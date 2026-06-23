package com.example.memories.domain.chat.service;

import com.example.memories.domain.chat.dto.request.ChatImageSendRequestDto;
import com.example.memories.domain.chat.dto.request.ChatSendRequestDto;
import com.example.memories.domain.chat.dto.response.ChatListResponseDto;
import com.example.memories.domain.chat.dto.response.ChatResponseDto;
import com.example.memories.domain.user.entity.User;

public interface ChatService {
    ChatResponseDto sendMessage(Long roomId, User user, ChatSendRequestDto request);
    ChatResponseDto sendImageMessage(Long roomId, User user, ChatImageSendRequestDto request);
    ChatListResponseDto getChats(Long roomId, User user, Long cursor, int size);
    void deleteChat(Long roomId, Long chatId, User user);
}
