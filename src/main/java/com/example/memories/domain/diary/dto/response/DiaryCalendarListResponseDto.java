package com.example.memories.domain.diary.dto.response;

import com.example.memories.domain.diary.entity.Diary;
import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.room.entity.enums.RoomType;
import com.example.memories.domain.user.entity.User;

import java.time.LocalDate;
import java.util.List;

public record DiaryCalendarListResponseDto(
        List<DiaryItem> content,
        Long nextCursor,
        boolean hasNext
) {
    public static DiaryCalendarListResponseDto of(List<DiaryItem> content, Long nextCursor, boolean hasNext) {
        return new DiaryCalendarListResponseDto(content, nextCursor, hasNext);
    }

    public record DiaryItem(
            Long diaryId,
            Long authorId,
            String authorName,
            String title,
            LocalDate writtenDate,
            RoomSummary room
    ) {
        public static DiaryItem from(Diary diary) {
            // 작성자가 탈퇴한 경우(user == null) 익명 표시
            User author = diary.getUser();
            return new DiaryItem(
                    diary.getId(),
                    author != null ? author.getId() : null,
                    author != null ? author.getName() : User.WITHDRAWN_NAME,
                    diary.getTitle(),
                    diary.getDiaryDate(),
                    RoomSummary.from(diary.getRoom())
            );
        }
    }

    public record RoomSummary(Long roomId, String roomName, RoomType roomType) {
        public static RoomSummary from(Room room) {
            return new RoomSummary(room.getId(), room.getTitle(), room.getType());
        }
    }
}