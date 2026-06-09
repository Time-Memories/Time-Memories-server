package com.example.memories.domain.comment.repository;

import com.example.memories.domain.comment.entity.Comment;
import com.example.memories.domain.diary.entity.Diary;
import com.example.memories.domain.room.entity.Room;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 일기에 달린 댓글을 작성 시간 순(id ASC)으로 커서 기반 조회
    // 작성자(user)는 탈퇴 시 soft delete(@SQLRestriction)로 필터링되므로 LEFT JOIN으로 댓글 자체는 유지
    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN FETCH c.user
            WHERE c.diary = :diary
              AND (:cursor IS NULL OR c.id > :cursor)
            ORDER BY c.id ASC
            """)
    List<Comment> findByDiaryCursor(@Param("diary") Diary diary,
                                    @Param("cursor") Long cursor,
                                    Pageable pageable);

    // 수정/삭제 권한 검사를 위해 작성자(user)와 일기-방(diary.room)을 함께 조회
    @EntityGraph(attributePaths = {"user", "diary", "diary.room"})
    Optional<Comment> findWithDiaryRoomById(Long id);

    // 벌크 삭제 후 1차 캐시(영속성 컨텍스트)와 DB 불일치 방지 위해 clearAutomatically 적용
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.diary = :diary")
    void deleteAllByDiary(@Param("diary") Diary diary);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Comment c WHERE c.diary.room = :room")
    void deleteAllByRoom(@Param("room") Room room);
}
