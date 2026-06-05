package com.example.memories.domain.diary.dto.response;

import com.example.memories.domain.diary.entity.Diary;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public record DiaryListResponseDto(
        List<DiaryDto> content,
        int pageNumber,
        int pageSize,
        int totalPages,
        long totalElements
) {
    public static DiaryListResponseDto of(Page<Diary> page, List<DiaryDto> content) {
        return new DiaryListResponseDto(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements()
        );
    }

    public record DiaryDto(
            Long diaryId,
            Long authorId,
            String authorName,
            String title,
            String thumbnailUrl,
            LocalDateTime createdAt
    ) {
        public static DiaryDto of(Diary diary, String thumbnailUrl) {
            return new DiaryDto(
                    diary.getId(),
                    diary.getUser().getId(),
                    diary.getUser().getName(),
                    diary.getTitle(),
                    thumbnailUrl,
                    diary.getCreatedAt()
            );
        }
    }
}