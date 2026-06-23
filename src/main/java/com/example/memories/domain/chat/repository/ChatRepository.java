package com.example.memories.domain.chat.repository;

import com.example.memories.domain.chat.entity.Chat;
import com.example.memories.domain.room.entity.Room;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat,Long> {
    Optional<Chat> findByIdAndRoom(Long chatId, Room room);

    // 특정 방의 채팅 목록 조회 (최신순)
    @Query("""
        select c
        from Chat c
        left join fetch c.user
        where c.room = :room
        order by c.id desc
        """)
    List<Chat> findByRoomOrderByIdDescWithUser(@Param("room") Room room,
                                               Pageable pageable);
    // 특정 방의 채팅 목록 조회 (커서 기반, 최신순)
    @Query("""
        select c
        from Chat c
        left join fetch c.user
        where c.room = :room
          and c.id < :cursor
        order by c.id desc
        """)
    List<Chat> findByRoomAndIdLessThanOrderByIdDescWithUser(@Param("room") Room room,
                                                            @Param("cursor") Long cursor,
                                                            Pageable pageable);
}
