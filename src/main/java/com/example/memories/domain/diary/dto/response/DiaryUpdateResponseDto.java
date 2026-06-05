package com.example.memories.domain.diary.dto.response;

import com.example.memories.domain.diary.entity.Diary;

import java.time.LocalDateTime;
import java.util.List;

public record DiaryUpdateResponseDto(
        AuthorDto author,
        String title,
        String content,
        List<String> imageUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DiaryUpdateResponseDto of(Diary diary, List<String> imageUrls) {
        return new DiaryUpdateResponseDto(
                new AuthorDto(diary.getUser().getId(), diary.getUser().getName()),
                diary.getTitle(),
                diary.getContents(),
                imageUrls,
                diary.getCreatedAt(),
                diary.getUpdatedAt()
        );
    }

    public record AuthorDto(Long userId, String nickname) {}
}