package com.example.memories.domain.diary.dto.response;

import com.example.memories.domain.diary.entity.Diary;
import com.example.memories.domain.user.entity.User;

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
        // 작성자가 탈퇴한 경우(user == null) 익명 표시
        User author = diary.getUser();
        return new DiaryDetailResponseDto(
                diary.getId(),
                author != null ? author.getId() : null,
                author != null ? author.getName() : User.WITHDRAWN_NAME,
                diary.getTitle(),
                diary.getContents(),
                diary.getDiaryDate(),
                imageUrls,
                diary.getCreatedAt(),
                diary.getUpdatedAt()
        );
    }
}