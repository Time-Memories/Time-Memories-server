package com.example.memories.domain.room.service;

import com.example.memories.domain.room.dto.request.RoomCreateRequest;
import com.example.memories.domain.room.dto.request.RoomJoinRequest;
import com.example.memories.domain.room.dto.request.RoomUpdateRequest;
import com.example.memories.domain.room.dto.response.*;
import com.example.memories.domain.user.entity.User;
import org.springframework.data.domain.Pageable;

public interface RoomService {
    RoomCreateResponse createRoom(User user, RoomCreateRequest request);
    RoomListResponse getRooms(User user, Pageable pageable);
    RoomDetailResponse getRoomDetail(User user, Long roomId);
    RoomUpdateResponse updateRoom(User user, Long roomId, RoomUpdateRequest request);
    void deleteRoom(User user, Long roomId);
    RoomJoinResponse joinRoom(User user, RoomJoinRequest request);
    void leaveRoom(User user, Long roomId);
    RoomMemberListResponse getRoomMembers(User user, Long roomId, Pageable pageable);
}
