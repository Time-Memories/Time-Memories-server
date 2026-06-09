package com.example.memories.global.exception;

import com.example.memories.domain.chat.dto.response.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

@Slf4j
@ControllerAdvice
public class WebSocketExceptionHandler {

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public ErrorResponseDto handleBusinessException(BusinessException e) {
        log.warn("WebSocket BusinessException 발생: code={}, message={}",
                e.getErrorCode().getCode(),
                e.getErrorCode().getMessage());

        return ErrorResponseDto.of(
                "BUSINESS_ERROR",
                e.getErrorCode().getMessage(),
                e.getErrorCode().getCode()
        );
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public ErrorResponseDto handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("WebSocket IllegalArgumentException 발생: {}", e.getMessage());

        return ErrorResponseDto.of(
                "VALIDATION_ERROR",
                e.getMessage(),
                "INVALID_ARGUMENT"
        );
    }

    // 모든 예외 처리
    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ErrorResponseDto handleException(Exception e) {
        log.error("WebSocket 예상치 못한 예외 발생", e);

        return ErrorResponseDto.of(
                "UNKNOWN_ERROR",
                "알 수 없는 오류가 발생했습니다.",
                "INTERNAL_ERROR"
        );
    }
}