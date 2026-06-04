package com.example.memories.domain.room.repository;

import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.room.entity.RoomUser;
import com.example.memories.domain.room.entity.enums.RoomRole;
import com.example.memories.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomUserRepository extends JpaRepository<RoomUser, Long> {
    boolean existsByRoomAndUser(Room room, User user);

    Optional<RoomUser> findByRoomAndUser(Room room, User user);

    Optional<RoomUser> findByRoomAndRole(Room room, RoomRole role);

    // 방장 위임 대상 조회 (가장 먼저 입장한 MEMBER)
    Optional<RoomUser> findFirstByRoomAndRoleOrderByIdAsc(Room room, RoomRole role);

    void deleteAllByRoom(Room room);

    // 특정 방의 멤버 목록 조회: 입장 순 ASC
    @Query("""
            SELECT ru
            FROM RoomUser ru
            JOIN FETCH ru.user
            WHERE ru.room = :room
              AND (:cursor IS NULL OR ru.id > :cursor)
            ORDER BY ru.id ASC
            """)
    List<RoomUser> findAllByRoomWithUserCursor(@Param("room") Room room,
                                               @Param("cursor") Long cursor,
                                               Pageable pageable);

    // 현재 유저가 참여한 방 목록 조회: 최신 방 DESC
    @Query("""
            SELECT ru
            FROM RoomUser ru
            JOIN FETCH ru.room
            WHERE ru.user = :user
              AND (:cursor IS NULL OR ru.room.id < :cursor)
            ORDER BY ru.room.id DESC
            """)
    List<RoomUser> findAllByUserWithRoomCursor(@Param("user") User user,
                                               @Param("cursor") Long cursor,
                                               Pageable pageable);
}
