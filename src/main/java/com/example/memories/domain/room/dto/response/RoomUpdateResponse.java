package com.example.memories.domain.room.dto.response;

import com.example.memories.domain.room.entity.Room;

public record RoomUpdateResponse(
        String title
) {
    public static RoomUpdateResponse from(Room room) {
        return new RoomUpdateResponse(
                room.getTitle()
        );
    }
}
