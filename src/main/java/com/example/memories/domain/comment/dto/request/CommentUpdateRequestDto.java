package com.example.memories.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentUpdateRequestDto(
        @NotBlank
        String content
) {}
