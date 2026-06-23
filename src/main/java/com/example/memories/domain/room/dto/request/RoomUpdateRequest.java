package com.example.memories.domain.room.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RoomUpdateRequest(
        @NotBlank(message = "방 제목은 필수입니다.")
        String title
) {
}
