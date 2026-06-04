package com.example.memories.domain.room.exception;

import com.example.memories.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RoomErrorCode implements ErrorCode {
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "요청하신 방을 찾을 수 없습니다."),
    ROOM_OWNER_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM_OWNER_NOT_FOUND", "요청하신 방의 방장을 찾을 수 없습니다"),
    ROOM_NOT_OWNER(HttpStatus.FORBIDDEN, "ROOM_NOT_OWNER", "방장만 사용할 수 있습니다."),
    ROOM_FORBIDDEN(HttpStatus.FORBIDDEN, "ROOM_FORBIDDEN", "요청하신 방에 속한 인원이 아닙니다."),
    ROOM_OWNER_CANNOT_LEAVE(HttpStatus.FORBIDDEN, "ROOM_OWNER_CANNOT_LEAVE", "방장은 방을 나갈 수 없습니다. (삭제만 가능합니다)"),
    PRIVATE_ROOM_CANNOT_JOIN(HttpStatus.FORBIDDEN, "PRIVATE_ROOM_CANNOT_JOIN", "개인 방은 코드로 입장할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}