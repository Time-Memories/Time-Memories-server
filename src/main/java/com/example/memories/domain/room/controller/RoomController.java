package com.example.memories.domain.room.controller;

import com.example.memories.domain.room.dto.request.RoomCreateRequest;
import com.example.memories.domain.room.dto.request.RoomJoinRequest;
import com.example.memories.domain.room.dto.request.RoomUpdateRequest;
import com.example.memories.domain.room.dto.response.*;
import com.example.memories.domain.room.service.RoomService;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.annotation.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Room", description = "방 API")
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "방 생성", description = "새로운 방을 생성합니다. 생성자는 자동으로 방장으로 등록됩니다.")
    @PostMapping
    public ResponseEntity<RoomCreateResponse> createRoom(
            @CurrentUser User user,
            @RequestBody @Valid RoomCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomService.createRoom(user, request));
    }

    @Operation(summary = "방 목록 조회", description = "현재 로그인한 유저가 참여 중인 방 목록을 커서 기반으로 조회합니다.")
    @GetMapping
    public ResponseEntity<RoomListResponse> getRooms(
            @CurrentUser User user,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(roomService.getRooms(user, cursor, size));
    }

    @Operation(summary = "방 상세 조회", description = "방 상세 정보를 조회합니다.")
    @GetMapping("/{room_id}")
    public ResponseEntity<RoomDetailResponse> getRoomDetail(
            @CurrentUser User user,
            @PathVariable("room_id") Long roomId
    ) {
        return ResponseEntity.ok(roomService.getRoomDetail(user, roomId));
    }

    @Operation(summary = "방 정보 수정", description = "방장이 방 제목을 수정합니다.")
    @PatchMapping("/{room_id}")
    public ResponseEntity<RoomUpdateResponse> updateRoom(
            @CurrentUser User user,
            @PathVariable("room_id") Long roomId,
            @RequestBody @Valid RoomUpdateRequest request
    ) {
        return ResponseEntity.ok(roomService.updateRoom(user, roomId, request));
    }

    @Operation(summary = "방 삭제", description = "방장이 방을 삭제합니다.")
    @DeleteMapping("/{room_id}")
    public ResponseEntity<Void> deleteRoom(
            @CurrentUser User user,
            @PathVariable("room_id") Long roomId
    ) {
        roomService.deleteRoom(user, roomId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "초대코드로 방 입장", description = "초대코드를 입력하여 방에 입장합니다.")
    @PostMapping("/join")
    public ResponseEntity<RoomJoinResponse> joinRoom(
            @CurrentUser User user,
            @RequestBody @Valid RoomJoinRequest request
    ) {
        return ResponseEntity.ok(roomService.joinRoom(user, request));
    }

    @Operation(summary = "방 나가기", description = "현재 로그인한 유저가 방을 나갑니다. 방장은 방을 나갈 수 없습니다.")
    @DeleteMapping("/{room_id}/leave")
    public ResponseEntity<Void> leaveRoom(
            @CurrentUser User user,
            @PathVariable("room_id") Long roomId
    ) {
        roomService.leaveRoom(user, roomId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "방 멤버 조회", description = "해당 방에 참여 중인 멤버 목록을 커서 기반으로 조회합니다.")
    @GetMapping("/{room_id}/members")
    public ResponseEntity<RoomMemberListResponse> getRoomMembers(
            @CurrentUser User user,
            @PathVariable("room_id") Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(roomService.getRoomMembers(user, roomId, cursor, size));
    }
}
