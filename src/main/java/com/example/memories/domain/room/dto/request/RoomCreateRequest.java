package com.example.memories.domain.room.dto.request;

import com.example.memories.domain.room.entity.enums.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomCreateRequest(
        @NotBlank(message = "방 제목은 필수입니다.")
        String title,

        @NotNull(message = "방 타입은 필수입니다.")
        RoomType type
) {
}
