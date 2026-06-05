package com.example.memories.domain.diary.dto.response;

import com.example.memories.domain.diary.entity.Diary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DiaryDetailResponseDto(
        Long diaryId,
        Long authorId,
        String authorName,
        String title,
        String content,
        LocalDate diaryDate,
        List<String> imageUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DiaryDetailResponseDto of(Diary diary, List<String> imageUrls) {
        return new DiaryDetailResponseDto(
                diary.getId(),
                diary.getUser().getId(),
                diary.getUser().getName(),
                diary.getTitle(),
                diary.getContents(),
                diary.getDiaryDate(),
                imageUrls,
                diary.getCreatedAt(),
                diary.getUpdatedAt()
        );
    }
}