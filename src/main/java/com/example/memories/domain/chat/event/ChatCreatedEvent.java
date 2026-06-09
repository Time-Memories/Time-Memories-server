package com.example.memories.domain.chat.event;

import com.example.memories.domain.chat.dto.response.ChatResponseDto;

public record ChatCreatedEvent(
        Long roomId,
        ChatResponseDto response
) {
}
