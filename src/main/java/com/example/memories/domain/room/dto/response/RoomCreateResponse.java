package com.example.memories.domain.room.dto.response;

import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.room.entity.enums.RoomType;

import java.time.LocalDateTime;

public record RoomCreateResponse(
        Long roomId,
        String title,
        RoomType type,
        String roomCode,
        LocalDateTime createdAt
) {
    public static RoomCreateResponse from(Room room) {
        return new RoomCreateResponse(
                room.getId(),
                room.getTitle(),
                room.getType(),
                room.getRoomCode(),
                room.getCreatedAt()
        );
    }
}
