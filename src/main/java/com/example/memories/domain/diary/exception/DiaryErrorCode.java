package com.example.memories.domain.diary.exception;

import com.example.memories.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DiaryErrorCode implements ErrorCode {
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "DIARY_NOT_FOUND", "일기를 찾을 수 없습니다."),
    DIARY_FORBIDDEN(HttpStatus.FORBIDDEN, "DIARY_FORBIDDEN", "해당 방의 멤버만 접근할 수 있습니다."),
    DIARY_NOT_AUTHOR(HttpStatus.FORBIDDEN, "DIARY_NOT_AUTHOR", "본인이 작성한 일기만 수정/삭제할 수 있습니다."),
    DIARY_AUTHOR_LEFT_ROOM(HttpStatus.FORBIDDEN, "DIARY_AUTHOR_LEFT_ROOM", "방을 탈퇴한 경우 일기를 수정/삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}