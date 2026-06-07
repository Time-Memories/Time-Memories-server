package com.example.memories.domain.comment.exception;

import com.example.memories.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT_FORBIDDEN", "해당 방의 멤버만 접근할 수 있습니다."),
    COMMENT_NOT_AUTHOR(HttpStatus.FORBIDDEN, "COMMENT_NOT_AUTHOR", "본인이 작성한 댓글만 수정/삭제할 수 있습니다."),
    COMMENT_AUTHOR_LEFT_ROOM(HttpStatus.FORBIDDEN, "COMMENT_AUTHOR_LEFT_ROOM", "방을 탈퇴한 경우 댓글을 수정/삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
