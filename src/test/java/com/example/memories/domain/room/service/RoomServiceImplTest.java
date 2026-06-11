package com.example.memories.domain.room.service;

import com.example.memories.domain.room.dto.request.RoomCreateRequest;
import com.example.memories.domain.room.dto.request.RoomJoinRequest;
import com.example.memories.domain.room.dto.request.RoomUpdateRequest;
import com.example.memories.domain.room.dto.response.*;
import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.room.entity.RoomUser;
import com.example.memories.domain.room.entity.enums.RoomRole;
import com.example.memories.domain.room.entity.enums.RoomType;
import com.example.memories.domain.room.exception.RoomErrorCode;
import com.example.memories.domain.diary.entity.Diary;
import com.example.memories.domain.diary.repository.DiaryRepository;
import com.example.memories.domain.room.repository.RoomRepository;
import com.example.memories.domain.room.repository.RoomUserRepository;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock DiaryRepository diaryRepository;
    @Mock com.example.memories.domain.comment.repository.CommentRepository commentRepository;
    @Mock RoomRepository roomRepository;
    @Mock RoomUserRepository roomUserRepository;

    @InjectMocks RoomServiceImpl roomService;

    @Test
    @DisplayName("방 생성 시 Room을 저장하고 생성자를 OWNER로 RoomUser에 저장한다")
    void createRoom_success() {
        // given
        User user = createUser(1L);
        RoomCreateRequest request = new RoomCreateRequest("테스트 방", RoomType.GROUP);

        given(roomRepository.existsByRoomCode(anyString())).willReturn(false);
        given(roomRepository.save(any(Room.class))).willAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            ReflectionTestUtils.setField(room, "id", 1L);
            return room;
        });

        // when
        RoomCreateResponse result = roomService.createRoom(user, request);

        // then
        assertThat(result.roomId()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("테스트 방");
        assertThat(result.type()).isEqualTo(RoomType.GROUP);
        assertThat(result.roomCode()).isNotBlank();

        then(roomRepository).should().save(any(Room.class));
        then(roomUserRepository).should().save(any(RoomUser.class));
    }

    @Test
    @DisplayName("내가 참여한 방 목록을 커서 기반으로 조회한다")
    void getRooms_success() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L, "테스트 방", RoomType.GROUP);
        RoomUser roomUser = createRoomUser(1L, room, user, RoomRole.OWNER);

        given(roomUserRepository.findAllByUserWithRoomCursor(
                eq(user),
                isNull(),
                eq(PageRequest.of(0, 11))
        )).willReturn(List.of(roomUser));

        // when
        RoomListResponse result = roomService.getRooms(user, null, 10);

        // then
        assertThat(result.rooms()).hasSize(1);
        assertThat(result.rooms().get(0).roomId()).isEqualTo(1L);
        assertThat(result.rooms().get(0).title()).isEqualTo("테스트 방");
        assertThat(result.nextCursor()).isNull();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("방 상세 조회 시 방 정보와 방장 정보를 반환한다")
    void getRoomDetail_success() {
        // given
        User user = createUser(1L);
        User owner = createUser(2L);
        Room room = createRoom(1L, "테스트 방", RoomType.GROUP);
        RoomUser ownerRoomUser = createRoomUser(1L, room, owner, RoomRole.OWNER);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);
        given(roomUserRepository.findByRoomAndRole(room, RoomRole.OWNER))
                .willReturn(Optional.of(ownerRoomUser));

        // when
        RoomDetailResponse result = roomService.getRoomDetail(user, 1L);

        // then
        assertThat(result.roomId()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("테스트 방");
        assertThat(result.owner().ownerId()).isEqualTo(2L);

        then(roomRepository)
                .should()
                .findById(1L);

        then(roomUserRepository)
                .should()
                .findByRoomAndRole(room, RoomRole.OWNER);
    }

    @Test
    @DisplayName("방 상세 조회 시 방이 없으면 ROOM_NOT_FOUND 예외 발생")
    void getRoomDetail_roomNotFound() {
        // given
        User user = createUser(1L);

        given(roomRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> roomService.getRoomDetail(user, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(RoomErrorCode.ROOM_NOT_FOUND));
    }

    @Test
    @DisplayName("방 수정 시 방장이면 제목을 수정한다")
    void updateRoom_success() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L, "기존 방", RoomType.GROUP);
        RoomUser roomUser = createRoomUser(1L, room, user, RoomRole.OWNER);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomUserRepository.findByRoomAndUser(room, user)).willReturn(Optional.of(roomUser));

        // when
        RoomUpdateResponse result = roomService.updateRoom(
                user,
                1L,
                new RoomUpdateRequest("수정된 방")
        );

        // then
        assertThat(result.title()).isEqualTo("수정된 방");
        assertThat(room.getTitle()).isEqualTo("수정된 방");
    }

    @Test
    @DisplayName("방 수정 시 방장이 아니면 ROOM_NOT_OWNER 예외 발생")
    void updateRoom_notOwner() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L, "테스트 방", RoomType.GROUP);
        RoomUser roomUser = createRoomUser(1L, room, user, RoomRole.MEMBER);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomUserRepository.findByRoomAndUser(room, user)).willReturn(Optional.of(roomUser));

        // when & then
        assertThatThrownBy(() -> roomService.updateRoom(user, 1L, new RoomUpdateRequest("수정")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(RoomErrorCode.ROOM_NOT_OWNER));
    }

    @Test
    @DisplayName("방 삭제 시 방장이면 RoomUser를 먼저 삭제하고 Room을 삭제한다")
    void deleteRoom_success() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L, "테스트 방", RoomType.GROUP);
        RoomUser roomUser = createRoomUser(1L, room, user, RoomRole.OWNER);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomUserRepository.findByRoomAndUser(room, user)).willReturn(Optional.of(roomUser));
        given(diaryRepository.findAllByRoom(room)).willReturn(List.of());

        // when
        roomService.deleteRoom(user, 1L);

        // then
        then(roomUserRepository).should().deleteAllByRoom(room);
        then(commentRepository).should().deleteAllByRoom(room);
        then(diaryRepository).should().deleteAll(List.of());
        then(roomRepository).should().delete(room);
    }

    @Test
    @DisplayName("초대코드로 방 입장 시 RoomUser를 MEMBER로 저장한다")
    void joinRoom_success() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L, "테스트 방", RoomType.GROUP);

        given(roomRepository.findByRoomCode("ABC123")).willReturn(Optional.of(room));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(false);

        // when
        RoomJoinResponse result = roomService.joinRoom(user, new RoomJoinRequest("ABC123"));

        // then
        assertThat(result.roomId()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("테스트 방");
        then(roomUserRepository).should().save(any(RoomUser.class));
    }

    @Test
    @DisplayName("이미 참여 중인 방이면 RoomUser를 새로 저장하지 않는다")
    void joinRoom_alreadyJoined() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L, "테스트 방", RoomType.GROUP);

        given(roomRepository.findByRoomCode("ABC123")).willReturn(Optional.of(room));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);

        // when
        RoomJoinResponse result = roomService.joinRoom(user, new RoomJoinRequest("ABC123"));

        // then
        assertThat(result.roomId()).isEqualTo(1L);
        then(roomUserRepository).should(never()).save(any(RoomUser.class));
    }

    @Test
    @DisplayName("방 나가기 시 MEMBER면 RoomUser를 삭제한다")
    void leaveRoom_success() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L, "테스트 방", RoomType.GROUP);
        RoomUser roomUser = createRoomUser(1L, room, user, RoomRole.MEMBER);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomUserRepository.findByRoomAndUser(room, user)).willReturn(Optional.of(roomUser));

        // when
        roomService.leaveRoom(user, 1L);

        // then
        then(roomUserRepository).should().delete(roomUser);
    }

    @Test
    @DisplayName("방장이 나가고 다른 멤버가 있으면 가장 먼저 들어온 멤버에게 방장을 위임한다")
    void leaveRoom_ownerLeave_transferOwner() {
        // given
        User owner = createUser(1L);
        User member = createUser(2L);
        Room room = createRoom(1L, "테스트 방", RoomType.GROUP);

        RoomUser ownerRoomUser = createRoomUser(1L, room, owner, RoomRole.OWNER);
        RoomUser memberRoomUser = createRoomUser(2L, room, member, RoomRole.MEMBER);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomUserRepository.findByRoomAndUser(room, owner)).willReturn(Optional.of(ownerRoomUser));
        given(roomUserRepository.findFirstByRoomAndRoleOrderByIdAsc(room, RoomRole.MEMBER))
                .willReturn(Optional.of(memberRoomUser));

        // when
        roomService.leaveRoom(owner, 1L);

        // then
        assertThat(memberRoomUser.getRole()).isEqualTo(RoomRole.OWNER);
        then(roomUserRepository).should().delete(ownerRoomUser);
        then(roomRepository).should(never()).delete(room);
    }

    @Test
    @DisplayName("방장이 혼자 남은 방에서 나가면 방을 삭제한다")
    void leaveRoom_ownerAlone_deleteRoom() {
        // given
        User owner = createUser(1L);
        Room room = createRoom(1L, "테스트 방", RoomType.GROUP);
        RoomUser ownerRoomUser = createRoomUser(1L, room, owner, RoomRole.OWNER);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomUserRepository.findByRoomAndUser(room, owner)).willReturn(Optional.of(ownerRoomUser));
        given(roomUserRepository.findFirstByRoomAndRoleOrderByIdAsc(room, RoomRole.MEMBER))
                .willReturn(Optional.empty());
        given(diaryRepository.findAllByRoom(room)).willReturn(List.of());

        // when
        roomService.leaveRoom(owner, 1L);

        // then
        then(roomUserRepository).should().deleteAllByRoom(room);
        then(commentRepository).should().deleteAllByRoom(room);
        then(diaryRepository).should().deleteAll(List.of());
        then(roomRepository).should().delete(room);
    }

    @Test
    @DisplayName("회원 탈퇴 시 가입한 모든 방을 퇴장 처리한다 - 일반 멤버 방은 RoomUser만 삭제(콘텐츠 유지), 혼자 남은 방장 방은 방 삭제")
    void leaveAllRooms_success() {
        // given
        User user = createUser(1L);

        // 다른 멤버가 있는 방의 일반 멤버 → RoomUser만 삭제
        Room memberRoom = createRoom(1L, "멤버로 있는 방", RoomType.GROUP);
        RoomUser memberRoomUser = createRoomUser(10L, memberRoom, user, RoomRole.MEMBER);

        // 혼자 남은 방장 방 → 방 삭제
        Room ownerRoom = createRoom(2L, "혼자 있는 방", RoomType.GROUP);
        RoomUser ownerRoomUser = createRoomUser(20L, ownerRoom, user, RoomRole.OWNER);

        given(roomUserRepository.findAllByUserWithRoom(user))
                .willReturn(List.of(memberRoomUser, ownerRoomUser));
        given(roomUserRepository.findFirstByRoomAndRoleOrderByIdAsc(ownerRoom, RoomRole.MEMBER))
                .willReturn(Optional.empty());
        given(diaryRepository.findAllByRoom(ownerRoom)).willReturn(List.of());

        // when
        roomService.leaveAllRooms(user);

        // then
        then(roomUserRepository).should().delete(memberRoomUser);
        then(roomUserRepository).should().deleteAllByRoom(ownerRoom);
        // 혼자 남은 방만 방+댓글+일기 삭제, 멤버로 있던 방은 콘텐츠 유지
        then(commentRepository).should().deleteAllByRoom(ownerRoom);
        then(roomRepository).should().delete(ownerRoom);
        then(commentRepository).should(never()).deleteAllByRoom(memberRoom);
        then(roomRepository).should(never()).delete(memberRoom);
    }

    @Test
    @DisplayName("회원 탈퇴 시 방장으로 있고 다른 멤버가 있는 방은 방장을 위임하고 RoomUser만 삭제한다")
    void leaveAllRooms_transferOwnership() {
        // given
        User user = createUser(1L);
        User member = createUser(2L);
        Room room = createRoom(1L, "방", RoomType.GROUP);
        RoomUser ownerRoomUser = createRoomUser(10L, room, user, RoomRole.OWNER);
        RoomUser memberRoomUser = createRoomUser(11L, room, member, RoomRole.MEMBER);

        given(roomUserRepository.findAllByUserWithRoom(user))
                .willReturn(List.of(ownerRoomUser));
        given(roomUserRepository.findFirstByRoomAndRoleOrderByIdAsc(room, RoomRole.MEMBER))
                .willReturn(Optional.of(memberRoomUser));

        // when
        roomService.leaveAllRooms(user);

        // then
        assertThat(memberRoomUser.getRole()).isEqualTo(RoomRole.OWNER);
        then(roomUserRepository).should().delete(ownerRoomUser);
        then(roomRepository).should(never()).delete(room);
        then(commentRepository).should(never()).deleteAllByRoom(room);
    }

    @Test
    @DisplayName("방 멤버 목록을 커서 기반으로 조회한다")
    void getRoomMembers_success() {
        // given
        User user = createUser(1L);
        User member = createUser(2L);
        Room room = createRoom(1L, "테스트 방", RoomType.GROUP);
        RoomUser memberRoomUser = createRoomUser(1L, room, member, RoomRole.MEMBER);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);
        given(roomUserRepository.findAllByRoomWithUserCursor(
                eq(room),
                isNull(),
                eq(PageRequest.of(0, 11))
        )).willReturn(List.of(memberRoomUser));

        // when
        RoomMemberListResponse result = roomService.getRoomMembers(user, 1L, null, 10);

        // then
        assertThat(result.members()).hasSize(1);
        assertThat(result.members().get(0).userId()).isEqualTo(2L);
        assertThat(result.members().get(0).name()).isEqualTo("user2");
        assertThat(result.members().get(0).role()).isEqualTo(RoomRole.MEMBER);
        assertThat(result.nextCursor()).isNull();
        assertThat(result.hasNext()).isFalse();
    }

    private User createUser(Long id) {
        User user = User.builder()
                .name("user" + id)
                .email("user" + id + "@test.com")
                .provider(AuthProvider.KAKAO)
                .providerId("provider-" + id)
                .build();

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }

    private Room createRoom(Long id, String title, RoomType type) {
        Room room = Room.builder()
                .title(title)
                .type(type)
                .roomCode("ABC123")
                .build();

        ReflectionTestUtils.setField(room, "id", id);

        return room;
    }

    private RoomUser createRoomUser(
            Long id,
            Room room,
            User user,
            RoomRole role
    ) {
        RoomUser roomUser = RoomUser.builder()
                .room(room)
                .user(user)
                .role(role)
                .build();

        ReflectionTestUtils.setField(roomUser, "id", id);

        return roomUser;
    }
}