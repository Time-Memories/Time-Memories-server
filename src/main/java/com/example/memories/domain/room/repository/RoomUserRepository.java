package com.example.memories.domain.room.repository;

import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.room.entity.RoomUser;
import com.example.memories.domain.room.entity.enums.RoomRole;
import com.example.memories.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RoomUserRepository extends JpaRepository<RoomUser, Long> {
    boolean existsByRoomAndUser(Room room, User user);

    Optional<RoomUser> findByRoomAndUser(Room room, User user);

    Optional<RoomUser> findByRoomAndRole(Room room, RoomRole role);

    void deleteAllByRoom(Room room);

    // 특정 방의 멤버 목록 조회
    // MemberDto 생성 시 user 정보가 필요하므로 fetch join
    @Query(value = """
            SELECT ru
            FROM RoomUser ru
            JOIN FETCH ru.user
            WHERE ru.room = :room
            """,
    countQuery = """
            SELECT COUNT(ru)
            FROM RoomUser ru
            WHERE ru.room = :room
            """)
    Page<RoomUser> findAllByRoomWithUser(Room room, Pageable pageable);

    // 현재 유저가 참여한 방 목록 조회
    // RoomDto 생성 시 room 정보가 필요하므로 fetch join
    @Query(value = """
            SELECT ru
            FROM RoomUser ru
            JOIN FETCH ru.room
            WHERE ru.user = :user
            """,
    countQuery = """
            SELECT COUNT(ru)
            FROM RoomUser ru
            WHERE ru.user = :user
            """
    )
    Page<RoomUser> findAllByUserWithRoom(User user, Pageable pageable);
}
