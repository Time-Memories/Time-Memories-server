package com.example.memories.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequestDto(
        @NotBlank
        String content
) {}
