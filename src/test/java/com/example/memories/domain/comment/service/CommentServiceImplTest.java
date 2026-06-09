package com.example.memories.domain.comment.service;

import com.example.memories.domain.comment.dto.request.CommentCreateRequestDto;
import com.example.memories.domain.comment.dto.request.CommentUpdateRequestDto;
import com.example.memories.domain.comment.dto.response.CommentListResponseDto;
import com.example.memories.domain.comment.dto.response.CommentResponseDto;
import com.example.memories.domain.comment.entity.Comment;
import com.example.memories.domain.comment.exception.CommentErrorCode;
import com.example.memories.domain.comment.repository.CommentRepository;
import com.example.memories.domain.diary.entity.Diary;
import com.example.memories.domain.diary.exception.DiaryErrorCode;
import com.example.memories.domain.diary.repository.DiaryRepository;
import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.room.entity.enums.RoomType;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock CommentRepository commentRepository;
    @Mock DiaryRepository diaryRepository;
    @Mock RoomUserRepository roomUserRepository;

    @InjectMocks CommentServiceImpl commentService;

    // ==================== 댓글 생성 ====================

    @Test
    @DisplayName("방 멤버가 일기에 댓글을 작성하면 댓글을 저장하고 반환한다")
    void createComment_success() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room);
        CommentCreateRequestDto request = new CommentCreateRequestDto("좋은 일기네요");

        given(diaryRepository.findWithRoomById(10L)).willReturn(Optional.of(diary));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);
        given(commentRepository.save(any(Comment.class))).willAnswer(inv -> {
            Comment c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 100L);
            return c;
        });

        // when
        CommentResponseDto result = commentService.createComment(user, 10L, request);

        // then
        assertThat(result.commentId()).isEqualTo(100L);
        assertThat(result.content()).isEqualTo("좋은 일기네요");
        then(commentRepository).should().save(any(Comment.class));
    }

    @Test
    @DisplayName("방 멤버가 아니면 댓글 작성 시 COMMENT_FORBIDDEN 예외가 발생한다")
    void createComment_notMember() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room);

        given(diaryRepository.findWithRoomById(10L)).willReturn(Optional.of(diary));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> commentService.createComment(user, 10L, new CommentCreateRequestDto("내용")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(CommentErrorCode.COMMENT_FORBIDDEN));
    }

    @Test
    @DisplayName("존재하지 않는 일기에 댓글을 작성하면 DIARY_NOT_FOUND 예외가 발생한다")
    void createComment_diaryNotFound() {
        // given
        User user = createUser(1L);
        given(diaryRepository.findWithRoomById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.createComment(user, 99L, new CommentCreateRequestDto("내용")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(DiaryErrorCode.DIARY_NOT_FOUND));
    }

    // ==================== 댓글 조회 ====================

    @Test
    @DisplayName("댓글 목록은 size+1 조회로 다음 페이지 여부와 커서를 계산한다")
    void getComments_hasNext() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room);

        Comment c1 = createComment(1L, diary, user, "댓글1");
        Comment c2 = createComment(2L, diary, user, "댓글2");
        Comment c3 = createComment(3L, diary, user, "댓글3");

        given(diaryRepository.findWithRoomById(10L)).willReturn(Optional.of(diary));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);
        // size=2 이므로 3개(size+1) 반환 → hasNext=true
        given(commentRepository.findByDiaryCursor(eq(diary), eq(null), any()))
                .willReturn(List.of(c1, c2, c3));

        // when
        CommentListResponseDto result = commentService.getComments(user, 10L, null, 2);

        // then
        assertThat(result.content()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(2L);
        assertThat(result.content().get(0).authorNickname()).isEqualTo("테스터1");
    }

    @Test
    @DisplayName("마지막 페이지는 hasNext가 false이고 nextCursor가 null이다")
    void getComments_lastPage() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room);
        Comment c1 = createComment(1L, diary, user, "댓글1");

        given(diaryRepository.findWithRoomById(10L)).willReturn(Optional.of(diary));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);
        given(commentRepository.findByDiaryCursor(eq(diary), eq(null), any()))
                .willReturn(List.of(c1));

        // when
        CommentListResponseDto result = commentService.getComments(user, 10L, null, 10);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("작성자가 탈퇴(user=null)한 댓글은 '탈퇴한 사용자'로 표시된다")
    void getComments_withdrawnAuthor() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room);
        Comment withdrawn = createComment(1L, diary, user, "탈퇴자 댓글");
        ReflectionTestUtils.setField(withdrawn, "user", null); // soft delete 후 LEFT JOIN 결과 user=null

        given(diaryRepository.findWithRoomById(10L)).willReturn(Optional.of(diary));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);
        given(commentRepository.findByDiaryCursor(eq(diary), eq(null), any()))
                .willReturn(List.of(withdrawn));

        // when
        CommentListResponseDto result = commentService.getComments(user, 10L, null, 10);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).authorId()).isNull();
        assertThat(result.content().get(0).authorNickname()).isEqualTo("탈퇴한 사용자");
    }

    // ==================== 댓글 수정 ====================

    @Test
    @DisplayName("작성자가 본인 댓글을 수정하면 내용이 변경된다")
    void updateComment_success() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room);
        Comment comment = createComment(100L, diary, user, "원본");

        given(commentRepository.findWithDiaryRoomById(100L)).willReturn(Optional.of(comment));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);

        // when
        CommentResponseDto result = commentService.updateComment(user, 100L, new CommentUpdateRequestDto("수정됨"));

        // then
        assertThat(result.content()).isEqualTo("수정됨");
        assertThat(comment.getContent()).isEqualTo("수정됨");
    }

    @Test
    @DisplayName("작성자가 아니면 댓글 수정 시 COMMENT_NOT_AUTHOR 예외가 발생한다")
    void updateComment_notAuthor() {
        // given
        User author = createUser(1L);
        User other = createUser(2L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, author, room);
        Comment comment = createComment(100L, diary, author, "원본");

        given(commentRepository.findWithDiaryRoomById(100L)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.updateComment(other, 100L, new CommentUpdateRequestDto("수정")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(CommentErrorCode.COMMENT_NOT_AUTHOR));
    }

    @Test
    @DisplayName("방을 탈퇴한 작성자가 댓글을 수정하면 COMMENT_AUTHOR_LEFT_ROOM 예외가 발생한다")
    void updateComment_authorLeftRoom() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room);
        Comment comment = createComment(100L, diary, user, "원본");

        given(commentRepository.findWithDiaryRoomById(100L)).willReturn(Optional.of(comment));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> commentService.updateComment(user, 100L, new CommentUpdateRequestDto("수정")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(CommentErrorCode.COMMENT_AUTHOR_LEFT_ROOM));
    }

    // ==================== 댓글 삭제 ====================

    @Test
    @DisplayName("작성자가 본인 댓글을 삭제한다")
    void deleteComment_success() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room);
        Comment comment = createComment(100L, diary, user, "댓글");

        given(commentRepository.findWithDiaryRoomById(100L)).willReturn(Optional.of(comment));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);

        // when
        commentService.deleteComment(user, 100L);

        // then
        then(commentRepository).should().delete(comment);
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 삭제하면 COMMENT_NOT_FOUND 예외가 발생한다")
    void deleteComment_notFound() {
        // given
        User user = createUser(1L);
        given(commentRepository.findWithDiaryRoomById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(user, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    // ==================== 헬퍼 메서드 ====================

    private User createUser(Long id) {
        User user = User.builder()
                .name("테스터" + id)
                .email("test" + id + "@test.com")
                .provider(AuthProvider.KAKAO)
                .providerId("kakao_" + id)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Room createRoom(Long id) {
        Room room = Room.builder()
                .title("테스트 방")
                .type(RoomType.GROUP)
                .roomCode("TESTCD")
                .build();
        ReflectionTestUtils.setField(room, "id", id);
        return room;
    }

    private Diary createDiary(Long id, User user, Room room) {
        Diary diary = Diary.builder()
                .user(user)
                .room(room)
                .title("제목")
                .contents("내용")
                .diaryDate(LocalDate.of(2026, 5, 15))
                .build();
        ReflectionTestUtils.setField(diary, "id", id);
        return diary;
    }

    private Comment createComment(Long id, Diary diary, User user, String content) {
        Comment comment = Comment.builder()
                .diary(diary)
                .user(user)
                .content(content)
                .build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
