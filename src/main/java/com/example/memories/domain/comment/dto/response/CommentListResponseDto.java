package com.example.memories.domain.comment.dto.response;

import com.example.memories.domain.comment.entity.Comment;
import com.example.memories.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record CommentListResponseDto(
        List<CommentItem> content,
        Long nextCursor,
        boolean hasNext
) {
    public static CommentListResponseDto of(List<CommentItem> content, Long nextCursor, boolean hasNext) {
        return new CommentListResponseDto(content, nextCursor, hasNext);
    }

    public record CommentItem(
            Long commentId,
            Long authorId,
            String authorNickname,
            String content,
            LocalDateTime createdAt
    ) {
        public static CommentItem from(Comment comment) {
            // 작성자가 탈퇴한 경우(user == null) 익명 표시
            User author = comment.getUser();
            return new CommentItem(
                    comment.getId(),
                    author != null ? author.getId() : null,
                    author != null ? author.getName() : User.WITHDRAWN_NAME,
                    comment.getContent(),
                    comment.getCreatedAt()
            );
        }
    }
}
