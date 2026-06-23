package com.example.memories.domain.room.dto.response;

import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.room.entity.enums.RoomType;
import com.example.memories.domain.user.entity.User;

import java.time.LocalDateTime;

public record RoomDetailResponse(
        Long roomId,
        String title,
        RoomType type,
        String roomCode,
        OwnerDto owner,
        LocalDateTime createdAt
) {
    public static RoomDetailResponse of(Room room, User owner) {
        return new RoomDetailResponse(
                room.getId(),
                room.getTitle(),
                room.getType(),
                room.getRoomCode(),
                OwnerDto.from(owner),
                room.getCreatedAt()
        );
    }

    public record OwnerDto(
            Long ownerId,
            String ownerName
    ) {
        public static OwnerDto from(User owner) {
            return new OwnerDto(
                    owner.getId(),
                    owner.getName()
            );
        }
    }
}
