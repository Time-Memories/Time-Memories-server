package com.example.memories.domain.room.dto.response;

import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.room.entity.enums.RoomType;

import java.time.LocalDateTime;
import java.util.List;

public record RoomListResponse(
        List<RoomDto> rooms,
        Long nextCursor,
        Boolean hasNext
) {
    public static RoomListResponse of(
            List<RoomDto> rooms,
            Long nextCursor,
            Boolean hasNext
    ) {
        return new RoomListResponse(
                rooms,
                nextCursor,
                hasNext
        );
    }

    public record RoomDto(
            Long roomId,
            String title,
            RoomType type,
            LocalDateTime createdAt
    ) {
        public static RoomDto from(Room room) {
            return new RoomDto(
                    room.getId(),
                    room.getTitle(),
                    room.getType(),
                    room.getCreatedAt()
            );
        }
    }
}
