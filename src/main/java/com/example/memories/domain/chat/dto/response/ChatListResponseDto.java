package com.example.memories.domain.chat.dto.response;

import java.util.List;

public record ChatListResponseDto(
        List<ChatResponseDto> messages,
        Boolean hasNext,
        Long nextCursor
) {
}
