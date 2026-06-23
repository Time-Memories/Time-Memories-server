package com.example.memories.domain.comment.repository;

import com.example.memories.domain.comment.entity.Comment;
import com.example.memories.domain.diary.entity.Diary;
import com.example.memories.domain.diary.repository.DiaryRepository;
import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.room.entity.enums.RoomType;
import com.example.memories.domain.room.repository.RoomRepository;
import com.example.memories.domain.user.entity.User;
import com.example.memories.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 핵심 가정 검증: 작성자(User)가 탈퇴(soft delete)해도
 * LEFT JOIN FETCH + @SQLRestriction 조합 덕분에 일기/댓글 자체는 조회되며 user만 null이 된다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:softdeltest;MODE=MYSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SoftDeletedAuthorQueryTest {

    @Autowired TestEntityManager em;
    @Autowired UserRepository userRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired DiaryRepository diaryRepository;
    @Autowired CommentRepository commentRepository;

    @Test
    @DisplayName("작성자가 탈퇴해도 댓글/일기는 조회되고 user만 null이 된다")
    void softDeletedAuthor_contentRetained_userNull() {
        // given - 유저/방/일기/댓글 저장
        User author = userRepository.save(User.builder()
                .name("작성자").email("a@test.com")
                .provider(com.example.memories.domain.user.entity.AuthProvider.KAKAO)
                .providerId("kakao-1").build());
        Room room = roomRepository.save(Room.builder()
                .title("방").type(RoomType.GROUP).roomCode("ABC123").build());
        Diary diary = diaryRepository.save(Diary.builder()
                .user(author).room(room).title("제목").contents("내용")
                .diaryDate(LocalDate.of(2026, 5, 15)).build());
        Comment comment = commentRepository.save(Comment.builder()
                .diary(diary).user(author).content("댓글").build());
        em.flush();
        em.clear(); // 일기/댓글이 author를 참조한 채로 영속화되어 있으면 탈퇴 flush가 깨지므로 분리

        // when - 작성자 탈퇴 (soft delete: deleted_at 세팅)
        User managedAuthor = userRepository.findById(author.getId()).orElseThrow();
        userRepository.delete(managedAuthor);
        em.flush();
        em.clear();

        // then - 댓글은 그대로 조회되며 user만 null
        List<Comment> comments = commentRepository.findByDiaryCursor(diary, null, PageRequest.of(0, 10));
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getId()).isEqualTo(comment.getId());
        assertThat(comments.get(0).getUser()).isNull();

        // then - 일기도 그대로 조회되며 user만 null
        Page<Diary> diaries = diaryRepository.findAllByRoomWithUser(room, PageRequest.of(0, 10));
        assertThat(diaries.getContent()).hasSize(1);
        assertThat(diaries.getContent().get(0).getId()).isEqualTo(diary.getId());
        assertThat(diaries.getContent().get(0).getUser()).isNull();
    }
}
