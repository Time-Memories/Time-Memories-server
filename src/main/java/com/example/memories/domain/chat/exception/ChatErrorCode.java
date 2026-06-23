package com.example.memories.domain.chat.exception;

import com.example.memories.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode{
    CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_NOT_FOUND", "요청하신 채팅을 찾을 수 없습니다."),
    CHAT_FORBIDDEN(HttpStatus.FORBIDDEN, "CHAT_FORBIDDEN", "요청하신 채팅의 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}


