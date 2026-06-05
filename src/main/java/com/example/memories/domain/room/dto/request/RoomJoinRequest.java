package com.example.memories.domain.room.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RoomJoinRequest(
        @NotBlank(message = "방 코드는 필수입니다.")
        String roomCode
) {
}
