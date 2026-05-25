package com.example.memories.domain.user.dto.response;

import com.example.memories.domain.user.entity.User;

public record UpdateUserResponseDto(
        Long userId,
        String name
) {
    public static UpdateUserResponseDto from(User user) {
        return new UpdateUserResponseDto(
                user.getId(),
                user.getName()
        );
    }
}
