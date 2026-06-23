package com.example.memories.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode code) {
        super(code.getMessage()); // 기본 메시지
        this.errorCode = code;
    }

    public BusinessException(ErrorCode code, String detail) {
        super(detail != null ? detail : code.getMessage()); // 상세 메시지로 덮어쓰기 가능
        this.errorCode = code;
    }
}
