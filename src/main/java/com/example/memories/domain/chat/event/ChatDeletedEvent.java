package com.example.memories.domain.chat.event;

import com.example.memories.domain.chat.dto.response.MessageDeletedResponseDto;

public record ChatDeletedEvent(
        Long roomId,
        MessageDeletedResponseDto response
) {
}
