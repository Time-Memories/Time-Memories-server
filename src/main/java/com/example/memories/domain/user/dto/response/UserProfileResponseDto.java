package com.example.memories.domain.user.dto.response;

import com.example.memories.domain.user.entity.User;

import java.time.LocalDateTime;

public record UserProfileResponseDto(
        Long userId,
        String email,
        String name,
        LocalDateTime createdAt
) {
    public static UserProfileResponseDto from(User user) {
        return new UserProfileResponseDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCreatedAt()
        );
    }
}
