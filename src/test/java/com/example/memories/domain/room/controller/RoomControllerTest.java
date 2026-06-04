package com.example.memories.domain.room.controller;

import com.example.memories.domain.room.dto.request.RoomCreateRequest;
import com.example.memories.domain.room.dto.request.RoomJoinRequest;
import com.example.memories.domain.room.dto.request.RoomUpdateRequest;
import com.example.memories.domain.room.dto.response.*;
import com.example.memories.domain.room.entity.enums.RoomRole;
import com.example.memories.domain.room.entity.enums.RoomType;
import com.example.memories.domain.room.service.RoomService;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock RoomService roomService;

    @InjectMocks RoomController roomController;

    private User buildUser(Long id) {
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .provider(AuthProvider.KAKAO)
                .providerId("kakao-id")
                .build();

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }

    @Test
    @DisplayName("방 생성 시 201 Created와 생성된 방 DTO를 반환한다")
    void createRoom_returnsCreated() {
        // given
        User user = buildUser(1L);
        RoomCreateRequest request = new RoomCreateRequest("테스트 방", RoomType.GROUP);

        RoomCreateResponse dto = new RoomCreateResponse(
                1L,
                "테스트 방",
                RoomType.GROUP,
                "ABC123",
                LocalDateTime.of(2026, 6, 4, 0, 0)
        );

        given(roomService.createRoom(user, request)).willReturn(dto);

        // when
        ResponseEntity<RoomCreateResponse> response =
                roomController.createRoom(user, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("방 목록 조회 시 200 OK와 방 목록 DTO를 반환한다")
    void getRooms_returnsOk() {
        // given
        User user = buildUser(1L);
        Pageable pageable = PageRequest.of(0, 10);

        RoomListResponse dto = new RoomListResponse(
                List.of(
                        new RoomListResponse.RoomDto(
                                1L,
                                "테스트 방",
                                RoomType.GROUP,
                                LocalDateTime.of(2026, 6, 4, 0, 0)
                        )
                ),
                0,
                10,
                false
        );

        given(roomService.getRooms(user, pageable)).willReturn(dto);

        // when
        ResponseEntity<RoomListResponse> response =
                roomController.getRooms(user, pageable);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("방 상세 조회 시 200 OK와 방 상세 DTO를 반환한다")
    void getRoomDetail_returnsOk() {
        // given
        User user = buildUser(1L);

        RoomDetailResponse dto = new RoomDetailResponse(
                1L,
                "테스트 방",
                RoomType.GROUP,
                "ABC123",
                new RoomDetailResponse.OwnerDto(1L, "Test User"),
                LocalDateTime.of(2026, 6, 4, 0, 0)
        );

        given(roomService.getRoomDetail(user, 1L)).willReturn(dto);

        // when
        ResponseEntity<RoomDetailResponse> response =
                roomController.getRoomDetail(user, 1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("방 정보 수정 시 200 OK와 수정된 방 DTO를 반환한다")
    void updateRoom_returnsOk() {
        // given
        User user = buildUser(1L);
        RoomUpdateRequest request = new RoomUpdateRequest("수정된 방");

        RoomUpdateResponse dto = new RoomUpdateResponse("수정된 방");

        given(roomService.updateRoom(user, 1L, request)).willReturn(dto);

        // when
        ResponseEntity<RoomUpdateResponse> response =
                roomController.updateRoom(user, 1L, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("방 삭제 시 204 No Content를 반환하고 서비스를 호출한다")
    void deleteRoom_returnsNoContent() {
        // given
        User user = buildUser(1L);

        // when
        ResponseEntity<Void> response =
                roomController.deleteRoom(user, 1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        then(roomService).should().deleteRoom(user, 1L);
    }

    @Test
    @DisplayName("초대코드로 방 입장 시 200 OK와 입장한 방 DTO를 반환한다")
    void joinRoom_returnsOk() {
        // given
        User user = buildUser(1L);
        RoomJoinRequest request = new RoomJoinRequest("ABC123");

        RoomJoinResponse dto = new RoomJoinResponse(
                1L,
                "테스트 방"
        );

        given(roomService.joinRoom(user, request)).willReturn(dto);

        // when
        ResponseEntity<RoomJoinResponse> response =
                roomController.joinRoom(user, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("방 나가기 시 204 No Content를 반환하고 서비스를 호출한다")
    void leaveRoom_returnsNoContent() {
        // given
        User user = buildUser(1L);

        // when
        ResponseEntity<Void> response =
                roomController.leaveRoom(user, 1L);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        then(roomService).should().leaveRoom(user, 1L);
    }

    @Test
    @DisplayName("방 멤버 조회 시 200 OK와 멤버 목록 DTO를 반환한다")
    void getRoomMembers_returnsOk() {
        // given
        User user = buildUser(1L);
        Pageable pageable = PageRequest.of(0, 10);

        RoomMemberListResponse dto = new RoomMemberListResponse(
                List.of(
                        new RoomMemberListResponse.MemberDto(
                                1L,
                                "Test User",
                                RoomRole.OWNER
                        )
                ),
                0,
                10,
                false
        );

        given(roomService.getRoomMembers(user, 1L, pageable)).willReturn(dto);

        // when
        ResponseEntity<RoomMemberListResponse> response =
                roomController.getRoomMembers(user, 1L, pageable);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }
}
