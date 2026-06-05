package com.example.memories.domain.room.dto.response;

import com.example.memories.domain.room.entity.Room;

public record RoomJoinResponse(
        Long roomId,
        String title
) {
    public static RoomJoinResponse from(Room room) {
        return new RoomJoinResponse(
                room.getId(),
                room.getTitle()
        );
    }
}
