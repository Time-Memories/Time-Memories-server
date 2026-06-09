package com.example.memories.domain.comment.dto.response;

import com.example.memories.domain.comment.entity.Comment;

public record CommentResponseDto(
        Long commentId,
        String content
) {
    public static CommentResponseDto from(Comment comment) {
        return new CommentResponseDto(comment.getId(), comment.getContent());
    }
}
