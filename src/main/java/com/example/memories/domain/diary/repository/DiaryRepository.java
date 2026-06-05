package com.example.memories.domain.diary.repository;

import com.example.memories.domain.diary.entity.Diary;
import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    @Query(value = "SELECT d FROM Diary d JOIN FETCH d.user WHERE d.room = :room ORDER BY d.id DESC",
           countQuery = "SELECT COUNT(d) FROM Diary d WHERE d.room = :room")
    Page<Diary> findAllByRoomWithUser(@Param("room") Room room, Pageable pageable);

    @EntityGraph(attributePaths = {"images", "user"})
    Optional<Diary> findWithImagesById(Long id);

    List<Diary> findAllByRoom(Room room);

    // 사용자가 속한 모든 방의 일기를 날짜별 카운트 (달력 마커용)
    // row: [LocalDate diaryDate, Long count]
    @Query("""
            SELECT d.diaryDate, COUNT(d)
            FROM Diary d
            JOIN RoomUser ru ON d.room = ru.room
            WHERE ru.user = :user
              AND d.diaryDate BETWEEN :startDate AND :endDate
            GROUP BY d.diaryDate
            ORDER BY d.diaryDate ASC
            """)
    List<Object[]> countDiariesByDateRange(@Param("user") User user,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    // 소속 방 전체 일기 목록 (특정 기간, 커서 기반)
    @Query("""
            SELECT DISTINCT d FROM Diary d
            JOIN FETCH d.room
            JOIN FETCH d.user
            JOIN RoomUser ru ON d.room = ru.room
            WHERE ru.user = :user
              AND d.diaryDate BETWEEN :startDate AND :endDate
              AND (:cursor IS NULL OR d.id < :cursor)
            ORDER BY d.id DESC
            """)
    List<Diary> findRoomDiariesByDateRangeCursor(@Param("user") User user,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("cursor") Long cursor,
                                                 Pageable pageable);
}