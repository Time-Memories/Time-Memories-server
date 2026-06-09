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
import com.example.memories.domain.comment.repository.CommentRepository;
import com.example.memories.domain.diary.entity.Diary;
import com.example.memories.domain.diary.repository.DiaryRepository;
import com.example.memories.domain.room.repository.RoomRepository;
import com.example.memories.domain.room.repository.RoomUserRepository;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private static final int ROOM_CODE_LENGTH = 6;

    private final DiaryRepository diaryRepository;
    private final CommentRepository commentRepository;
    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;

    @Transactional
    @Override
    public RoomCreateResponse createRoom(User user, RoomCreateRequest request) {
        // 방 코드 생성
        String roomCode = generateRoomCode();

        Room room = Room.builder()
                .title(request.title())
                .type(request.type())
                .roomCode(roomCode)
                .build();

        // Room 저장
        Room savedRoom = roomRepository.save(room);

        RoomUser roomUser = RoomUser.builder()
                .room(savedRoom)
                .user(user)
                .role(RoomRole.OWNER)
                .build();

        // RoomUser 저장
        roomUserRepository.save(roomUser);

        return RoomCreateResponse.from(savedRoom);
    }

    @Override
    public RoomListResponse getRooms(User user, Long cursor, int size) {
        // 현재 유저가 참여한 방 목록 조회
        List<RoomUser> roomUsers = roomUserRepository.findAllByUserWithRoomCursor(
                user,
                cursor,
                PageRequest.of(0, size + 1)
        );

        // 조회된 데이터가 요청 개수보다 많으면 다음 페이지 존재
        // 다음 페이지 존재 시 마지막 데이터는 hasNext 확인용이므로 제외
        boolean hasNext = roomUsers.size() > size;
        List<RoomUser> content = hasNext
                ? roomUsers.subList(0, size)
                : roomUsers;

        // RoomUser -> RoomDto 변환
        List<RoomListResponse.RoomDto> rooms = content.stream()
                .map(RoomUser::getRoom)
                .map(RoomListResponse.RoomDto::from)
                .toList();

        Long nextCursor = hasNext
                ? content.get(content.size() - 1).getRoom().getId()
                : null;

        return RoomListResponse.of(
                rooms,
                nextCursor,
                hasNext
        );
    }

    @Override
    public RoomDetailResponse getRoomDetail(User user, Long roomId) {
        // 요청한 방 조회
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

        // 현재 유저가 해당 방에 속한 인원인지 검사
        if (!roomUserRepository.existsByRoomAndUser(room, user)) {
            throw new BusinessException(RoomErrorCode.ROOM_FORBIDDEN);
        }

        // 요청한 방의 방장이 있는지 검사 후 정보 가져오기
        RoomUser ownerRoomUser = roomUserRepository.findByRoomAndRole(room, RoomRole.OWNER)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_OWNER_NOT_FOUND));

        return RoomDetailResponse.of(room, ownerRoomUser.getUser());
    }

    @Transactional
    @Override
    public RoomUpdateResponse updateRoom(User user, Long roomId, RoomUpdateRequest request) {
        // 수정할 방 조회
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

        // 현재 유저가 해당 방에 속한 인원인지 검사
        RoomUser roomUser = roomUserRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_FORBIDDEN));

        // 방장만 방 수정 가능
        if (!roomUser.isOwner()) {
            throw new BusinessException(RoomErrorCode.ROOM_NOT_OWNER);
        }

        // 방 수정
        room.update(request.title());

        // 수정된 방 정보 반환
        return RoomUpdateResponse.from(room);
    }

    @Transactional
    @Override
    public void deleteRoom(User user, Long roomId) {
        // 삭제할 방 조회
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

        // 현재 유저가 해당 방에 속한 인원인지 검사
        RoomUser roomUser = roomUserRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_FORBIDDEN));

        // 방장만 방 삭제 가능
        if (!roomUser.isOwner()) {
            throw new BusinessException(RoomErrorCode.ROOM_NOT_OWNER);
        }

        // 방에 속한 RoomUser 먼저 삭제
        roomUserRepository.deleteAllByRoom(room);

        // 방에 속한 일기의 댓글 삭제 (일기보다 먼저 삭제해야 FK 제약 위반 없음)
        commentRepository.deleteAllByRoom(room);

        // 방에 속한 일기 삭제 (DiaryImage는 Diary의 cascade로 함께 삭제됨)
        List<Diary> diaries = diaryRepository.findAllByRoom(room);
        diaryRepository.deleteAll(diaries);

        // 방 삭제
        roomRepository.delete(room);
    }

    @Transactional
    @Override
    public RoomJoinResponse joinRoom(User user, RoomJoinRequest request) {
        // 참여할 방 조회
        Room room = roomRepository.findByRoomCode(request.roomCode())
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

        if (room.getType() == RoomType.PRIVATE) {
            throw new BusinessException(RoomErrorCode.PRIVATE_ROOM_CANNOT_JOIN);
        }

        // 이미 참여 중인 방이면 그대로 방 정보 반환
        if (roomUserRepository.existsByRoomAndUser(room, user)) {
            return RoomJoinResponse.from(room);
        }

        // 참여 중이 아니라면 일반 멤버로 RoomUser 생성
        RoomUser roomUser = RoomUser.builder()
                .room(room)
                .user(user)
                .role(RoomRole.MEMBER)
                .build();

        roomUserRepository.save(roomUser);

        return RoomJoinResponse.from(room);
    }

    @Transactional
    @Override
    public void leaveRoom(User user, Long roomId) {
        // 나갈 방 조회
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

        // 현재 유저가 해당 방에 속한 인원인지 검사
        RoomUser roomUser = roomUserRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_FORBIDDEN));

        leaveRoomInternal(room, roomUser);
    }

    @Transactional
    @Override
    public void leaveAllRooms(User user) {
        // 회원 탈퇴 시 가입한 모든 방에서 퇴장 처리
        // (방장이면 위임, 마지막 멤버면 방+일기+댓글 삭제, 일반 멤버면 RoomUser만 제거하고 작성 콘텐츠는 유지)
        List<RoomUser> roomUsers = roomUserRepository.findAllByUserWithRoom(user);
        for (RoomUser roomUser : roomUsers) {
            leaveRoomInternal(roomUser.getRoom(), roomUser);
        }
    }

    // 한 방에서의 퇴장 처리 (leaveRoom/leaveAllRooms 공통 로직)
    private void leaveRoomInternal(Room room, RoomUser roomUser) {
        // 일반 멤버는 바로 나가기 (작성한 일기/댓글은 방에 그대로 유지)
        if (!roomUser.isOwner()) {
            roomUserRepository.delete(roomUser);
            return;
        }

        // 방장인 경우, 가장 먼저 들어온 일반 멤버를 다음 방장으로 조회
        RoomUser nextOwner = roomUserRepository
                .findFirstByRoomAndRoleOrderByIdAsc(room, RoomRole.MEMBER)
                .orElse(null);

        // 방장이 혼자 남은 경우 방 삭제
        if (nextOwner == null) {
            roomUserRepository.delete(roomUser);
            // 방에 속한 일기의 댓글 삭제 (일기보다 먼저 삭제해야 FK 제약 위반 없음)
            commentRepository.deleteAllByRoom(room);
            // 방에 속한 일기 삭제 (DiaryImage는 Diary의 cascade로 함께 삭제됨)
            List<Diary> diaries = diaryRepository.findAllByRoom(room);
            diaryRepository.deleteAll(diaries);
            roomRepository.delete(room);
            return;
        }

        // 다른 멤버가 있으면 방장 위임 후 기존 방장 삭제
        nextOwner.changeRole(RoomRole.OWNER);
        roomUserRepository.delete(roomUser);
    }

    @Override
    public RoomMemberListResponse getRoomMembers(User user, Long roomId, Long cursor, int size) {
        // 방 조회
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

        // 현재 유저가 해당 방에 속한 인원인지 검사
        if (!roomUserRepository.existsByRoomAndUser(room, user)) {
            throw new BusinessException(RoomErrorCode.ROOM_FORBIDDEN);
        }

        // 해당 방의 멤버 목록 조회
        List<RoomUser> roomUsers = roomUserRepository.findAllByRoomWithUserCursor(
                room,
                cursor,
                PageRequest.of(0, size + 1)
        );

        // 조회된 데이터가 요청 개수보다 많으면 다음 페이지 존재
        // 다음 페이지 존재 시 마지막 데이터는 hasNext 확인용이므로 제외
        boolean hasNext = roomUsers.size() > size;
        List<RoomUser> content = hasNext
                ? roomUsers.subList(0, size)
                : roomUsers;

        // RoomUser -> MemberDto 변환
        List<RoomMemberListResponse.MemberDto> members = content.stream()
                .map(RoomMemberListResponse.MemberDto::from)
                .toList();

        // 다음 페이지가 존재하면 마지막 RoomUser의 ID를 다음 Cursor로 사용
        Long nextCursor = hasNext
                ? content.get(content.size() - 1).getId()
                : null;

        return RoomMemberListResponse.of(
                members,
                nextCursor,
                hasNext
        );
    }


    // 방 코드 생성 메서드
    private String generateRoomCode() {
        String roomCode;

        do {
            roomCode = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, ROOM_CODE_LENGTH)
                    .toUpperCase();
        } while (roomRepository.existsByRoomCode(roomCode));

        return roomCode;
    }
}
