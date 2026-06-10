    package com.example.memories.domain.chat.dto.response;

    import com.example.memories.domain.chat.entity.Chat;
    import com.example.memories.domain.chat.dto.ChatType;

    import java.time.LocalDateTime;
    import java.util.List;

    public record ChatResponseDto(
            Long chatId,
            Long senderId,
            String senderName,
            ChatType type,
            String content,
            List<String> imageKeys,
            LocalDateTime createdAt
    ) {
        public static ChatResponseDto from(Chat chat, List<String> imageKeys) {
            // 이미지 키가 있으면 이미지, 아니면 텍스트
            ChatType type = imageKeys.isEmpty()
                    ? ChatType.TEXT
                    : ChatType.IMAGE;

            return new ChatResponseDto(
                    chat.getId(),
                    chat.getUserId(),
                    chat.getUserName(),
                    type,
                    chat.getContent(),
                    imageKeys,
                    chat.getCreatedAt()
            );
        }
    }
