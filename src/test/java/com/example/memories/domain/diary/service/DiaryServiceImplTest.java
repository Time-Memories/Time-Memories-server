package com.example.memories.domain.diary.service;

import com.example.memories.domain.diary.dto.request.DiaryCreateRequestDto;
import com.example.memories.domain.diary.dto.request.DiaryUpdateRequestDto;
import com.example.memories.domain.diary.dto.response.CalendarCountResponseDto;
import com.example.memories.domain.diary.dto.response.DiaryCalendarListResponseDto;
import com.example.memories.domain.diary.dto.response.DiaryDetailResponseDto;
import com.example.memories.domain.diary.dto.response.DiaryListResponseDto;
import com.example.memories.domain.diary.dto.response.DiaryUpdateResponseDto;
import com.example.memories.domain.diary.entity.Diary;
import com.example.memories.domain.diary.entity.DiaryImage;
import com.example.memories.domain.diary.exception.DiaryErrorCode;
import com.example.memories.domain.diary.repository.DiaryImageRepository;
import com.example.memories.domain.diary.repository.DiaryRepository;
import com.example.memories.domain.room.entity.Room;
import com.example.memories.domain.room.entity.enums.RoomType;
import com.example.memories.domain.room.exception.RoomErrorCode;
import com.example.memories.domain.room.repository.RoomRepository;
import com.example.memories.domain.room.repository.RoomUserRepository;
import com.example.memories.domain.user.entity.AuthProvider;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.infra.s3.S3ImageDeleteEvent;
import com.example.memories.infra.s3.S3PresignService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class DiaryServiceImplTest {

    @Mock DiaryRepository diaryRepository;
    @Mock DiaryImageRepository diaryImageRepository;
    @Mock com.example.memories.domain.comment.repository.CommentRepository commentRepository;
    @Mock RoomRepository roomRepository;
    @Mock RoomUserRepository roomUserRepository;
    @Mock S3PresignService s3PresignService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks DiaryServiceImpl diaryService;

    // ==================== 일기 생성 ====================

    @Test
    @DisplayName("방 멤버가 이미지와 함께 일기를 생성하면 일기와 이미지 URL을 반환한다")
    void createDiary_success() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        DiaryCreateRequestDto request = new DiaryCreateRequestDto("제목", "내용", LocalDate.of(2026, 5, 15), List.of("img/key1.jpg", "img/key2.jpg"));

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);
        given(diaryRepository.save(any(Diary.class))).willAnswer(inv -> {
            Diary d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", 10L);
            return d;
        });
        given(s3PresignService.resolveImageUrl("img/key1.jpg")).willReturn("https://cdn/img/key1.jpg");
        given(s3PresignService.resolveImageUrl("img/key2.jpg")).willReturn("https://cdn/img/key2.jpg");

        // when
        DiaryDetailResponseDto result = diaryService.createDiary(user, 1L, request);

        // then
        assertThat(result.diaryId()).isEqualTo(10L);
        assertThat(result.title()).isEqualTo("제목");
        assertThat(result.content()).isEqualTo("내용");
        assertThat(result.imageUrls()).containsExactly("https://cdn/img/key1.jpg", "https://cdn/img/key2.jpg");
        then(diaryRepository).should().save(any(Diary.class));
    }

    @Test
    @DisplayName("일기 생성 시 방이 존재하지 않으면 ROOM_NOT_FOUND 예외가 발생한다")
    void createDiary_roomNotFound() {
        // given
        User user = createUser(1L);
        DiaryCreateRequestDto request = new DiaryCreateRequestDto("제목", "내용", LocalDate.of(2026, 5, 15), List.of());

        given(roomRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> diaryService.createDiary(user, 99L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(RoomErrorCode.ROOM_NOT_FOUND));
    }

    @Test
    @DisplayName("일기 생성 시 방 멤버가 아니면 DIARY_FORBIDDEN 예외가 발생한다")
    void createDiary_notRoomMember() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        DiaryCreateRequestDto request = new DiaryCreateRequestDto("제목", "내용", LocalDate.of(2026, 5, 15), List.of());

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> diaryService.createDiary(user, 1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(DiaryErrorCode.DIARY_FORBIDDEN));
    }

    // ==================== 방별 일기 목록 조회 ====================

    @Test
    @DisplayName("방 멤버가 일기 목록을 조회하면 썸네일 URL을 포함한 페이지를 반환한다")
    void getDiaries_success() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room, "제목");
        Page<Diary> page = new PageImpl<>(List.of(diary), PageRequest.of(0, 10), 1);

        DiaryImage thumbnail = createDiaryImage(1L, diary, "img/thumb.jpg", 0);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);
        given(diaryRepository.findAllByRoomWithUser(eq(room), any())).willReturn(page);
        given(diaryImageRepository.findThumbnailsByDiaries(any())).willReturn(List.of(thumbnail));
        given(s3PresignService.resolveImageUrl("img/thumb.jpg")).willReturn("https://cdn/img/thumb.jpg");

        // when
        DiaryListResponseDto result = diaryService.getDiaries(user, 1L, PageRequest.of(0, 10));

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).diaryId()).isEqualTo(10L);
        assertThat(result.content().get(0).thumbnailUrl()).isEqualTo("https://cdn/img/thumb.jpg");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("일기 목록 조회 시 방이 존재하지 않으면 ROOM_NOT_FOUND 예외가 발생한다")
    void getDiaries_roomNotFound() {
        // given
        User user = createUser(1L);
        given(roomRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> diaryService.getDiaries(user, 99L, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(RoomErrorCode.ROOM_NOT_FOUND));
    }

    @Test
    @DisplayName("일기 목록 조회 시 방 멤버가 아니면 DIARY_FORBIDDEN 예외가 발생한다")
    void getDiaries_notRoomMember() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> diaryService.getDiaries(user, 1L, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(DiaryErrorCode.DIARY_FORBIDDEN));
    }

    // ==================== 일기 상세 조회 ====================

    @Test
    @DisplayName("방 멤버가 일기를 상세 조회하면 이미지 URL 목록을 포함한 응답을 반환한다")
    void getDiary_success() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room, "여행 일기");
        diary.getImages().add(createDiaryImage(1L, diary, "img/a.jpg", 0));

        given(diaryRepository.findWithImagesById(10L)).willReturn(Optional.of(diary));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);
        given(s3PresignService.resolveImageUrl("img/a.jpg")).willReturn("https://cdn/img/a.jpg");

        // when
        DiaryDetailResponseDto result = diaryService.getDiary(user, 10L);

        // then
        assertThat(result.diaryId()).isEqualTo(10L);
        assertThat(result.title()).isEqualTo("여행 일기");
        assertThat(result.imageUrls()).containsExactly("https://cdn/img/a.jpg");
    }

    @Test
    @DisplayName("존재하지 않는 일기를 상세 조회하면 DIARY_NOT_FOUND 예외가 발생한다")
    void getDiary_diaryNotFound() {
        // given
        User user = createUser(1L);
        given(diaryRepository.findWithImagesById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> diaryService.getDiary(user, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(DiaryErrorCode.DIARY_NOT_FOUND));
    }

    @Test
    @DisplayName("일기 상세 조회 시 방 멤버가 아니면 DIARY_FORBIDDEN 예외가 발생한다")
    void getDiary_notRoomMember() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room, "여행 일기");

        given(diaryRepository.findWithImagesById(10L)).willReturn(Optional.of(diary));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> diaryService.getDiary(user, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(DiaryErrorCode.DIARY_FORBIDDEN));
    }

    // ==================== 일기 수정 ====================

    @Test
    @DisplayName("작성자가 일기를 수정하면 제거된 이미지는 S3에서 삭제되고 수정된 일기를 반환한다")
    void updateDiary_success() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room, "기존 제목");
        diary.getImages().add(createDiaryImage(1L, diary, "img/keep.jpg", 0));
        diary.getImages().add(createDiaryImage(2L, diary, "img/remove.jpg", 1));

        DiaryUpdateRequestDto request = new DiaryUpdateRequestDto("새 제목", "새 내용", LocalDate.of(2026, 6, 1), List.of("img/keep.jpg", "img/new.jpg"));

        given(diaryRepository.findWithImagesById(10L)).willReturn(Optional.of(diary));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);
        given(s3PresignService.resolveImageUrl("img/keep.jpg")).willReturn("https://cdn/img/keep.jpg");
        given(s3PresignService.resolveImageUrl("img/new.jpg")).willReturn("https://cdn/img/new.jpg");

        // when
        DiaryUpdateResponseDto result = diaryService.updateDiary(user, 10L, request);

        // then
        ArgumentCaptor<S3ImageDeleteEvent> captor = ArgumentCaptor.forClass(S3ImageDeleteEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        assertThat(captor.getValue().imageKeys()).containsExactly("img/remove.jpg");
        assertThat(result.title()).isEqualTo("새 제목");
        assertThat(result.content()).isEqualTo("새 내용");
        assertThat(result.imageUrls()).containsExactly("https://cdn/img/keep.jpg", "https://cdn/img/new.jpg");
    }

    @Test
    @DisplayName("존재하지 않는 일기를 수정하면 DIARY_NOT_FOUND 예외가 발생한다")
    void updateDiary_diaryNotFound() {
        // given
        User user = createUser(1L);
        DiaryUpdateRequestDto request = new DiaryUpdateRequestDto("새 제목", "새 내용", LocalDate.of(2026, 6, 1), List.of());

        given(diaryRepository.findWithImagesById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> diaryService.updateDiary(user, 99L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(DiaryErrorCode.DIARY_NOT_FOUND));
    }

    @Test
    @DisplayName("작성자가 방을 탈퇴한 경우 일기를 수정하려 하면 DIARY_AUTHOR_LEFT_ROOM 예외가 발생한다")
    void updateDiary_authorLeftRoom() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room, "제목");
        DiaryUpdateRequestDto request = new DiaryUpdateRequestDto("새 제목", "새 내용", LocalDate.of(2026, 6, 1), List.of());

        given(diaryRepository.findWithImagesById(10L)).willReturn(Optional.of(diary));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> diaryService.updateDiary(user, 10L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(DiaryErrorCode.DIARY_AUTHOR_LEFT_ROOM));
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 일기를 수정하려 하면 DIARY_NOT_AUTHOR 예외가 발생한다")
    void updateDiary_notAuthor() {
        // given
        User author = createUser(1L);
        User other = createUser(2L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, author, room, "제목");
        DiaryUpdateRequestDto request = new DiaryUpdateRequestDto("새 제목", "새 내용", LocalDate.of(2026, 6, 1), List.of());

        given(diaryRepository.findWithImagesById(10L)).willReturn(Optional.of(diary));

        // when & then
        assertThatThrownBy(() -> diaryService.updateDiary(other, 10L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(DiaryErrorCode.DIARY_NOT_AUTHOR));
    }

    // ==================== 일기 삭제 ====================

    @Test
    @DisplayName("작성자가 일기를 삭제하면 S3 이미지도 함께 삭제된다")
    void deleteDiary_success() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room, "제목");
        diary.getImages().add(createDiaryImage(1L, diary, "img/a.jpg", 0));
        diary.getImages().add(createDiaryImage(2L, diary, "img/b.jpg", 1));

        given(diaryRepository.findWithImagesById(10L)).willReturn(Optional.of(diary));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(true);

        // when
        diaryService.deleteDiary(user, 10L);

        // then
        ArgumentCaptor<S3ImageDeleteEvent> captor = ArgumentCaptor.forClass(S3ImageDeleteEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        assertThat(captor.getValue().imageKeys()).containsExactlyInAnyOrder("img/a.jpg", "img/b.jpg");
        then(commentRepository).should().deleteAllByDiary(diary);
        then(diaryRepository).should().delete(diary);
    }

    @Test
    @DisplayName("존재하지 않는 일기를 삭제하면 DIARY_NOT_FOUND 예외가 발생한다")
    void deleteDiary_diaryNotFound() {
        // given
        User user = createUser(1L);
        given(diaryRepository.findWithImagesById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> diaryService.deleteDiary(user, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(DiaryErrorCode.DIARY_NOT_FOUND));
    }

    @Test
    @DisplayName("작성자가 방을 탈퇴한 경우 일기를 삭제하려 하면 DIARY_AUTHOR_LEFT_ROOM 예외가 발생한다")
    void deleteDiary_authorLeftRoom() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, user, room, "제목");

        given(diaryRepository.findWithImagesById(10L)).willReturn(Optional.of(diary));
        given(roomUserRepository.existsByRoomAndUser(room, user)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> diaryService.deleteDiary(user, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(DiaryErrorCode.DIARY_AUTHOR_LEFT_ROOM));
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 일기를 삭제하려 하면 DIARY_NOT_AUTHOR 예외가 발생한다")
    void deleteDiary_notAuthor() {
        // given
        User author = createUser(1L);
        User other = createUser(2L);
        Room room = createRoom(1L);
        Diary diary = createDiary(10L, author, room, "제목");

        given(diaryRepository.findWithImagesById(10L)).willReturn(Optional.of(diary));

        // when & then
        assertThatThrownBy(() -> diaryService.deleteDiary(other, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(DiaryErrorCode.DIARY_NOT_AUTHOR));
    }

    // ==================== 달력 현황 조회 ====================

    @Test
    @DisplayName("월별 일기 작성 현황을 조회하면 날짜별 카운트 목록을 반환한다")
    void getCalendarCounts_success() {
        // given
        User user = createUser(1L);
        List<Object[]> rows = List.of(
                new Object[]{LocalDate.of(2026, 5, 8), 1L},
                new Object[]{LocalDate.of(2026, 5, 15), 2L}
        );

        given(diaryRepository.countDiariesByDateRange(eq(user), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(rows);

        // when
        CalendarCountResponseDto result = diaryService.getCalendarCounts(user, 2026, 5);

        // then
        assertThat(result.year()).isEqualTo(2026);
        assertThat(result.month()).isEqualTo(5);
        assertThat(result.writtenDates()).hasSize(2);
        assertThat(result.writtenDates().get(0).date()).isEqualTo(LocalDate.of(2026, 5, 8));
        assertThat(result.writtenDates().get(0).count()).isEqualTo(1L);
        assertThat(result.writtenDates().get(1).date()).isEqualTo(LocalDate.of(2026, 5, 15));
        assertThat(result.writtenDates().get(1).count()).isEqualTo(2L);
    }

    // ==================== 소속 방 일기 모아보기 ====================

    @Test
    @DisplayName("소속 방 일기를 월 전체 조회하면 hasNext=false와 content를 반환한다")
    void getMyDiaries_monthView_hasNextFalse() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary1 = createDiary(10L, user, room, "일기1");
        Diary diary2 = createDiary(9L, user, room, "일기2");

        given(diaryRepository.findRoomDiariesByDateRangeCursor(
                eq(user), any(LocalDate.class), any(LocalDate.class), isNull(), any()))
                .willReturn(List.of(diary1, diary2));

        // when
        DiaryCalendarListResponseDto result = diaryService.getMyDiaries(user, 2026, 5, null, null, 10);

        // then
        assertThat(result.content()).hasSize(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("소속 방 일기를 특정일 조회 시 size보다 많으면 hasNext=true와 nextCursor를 반환한다")
    void getMyDiaries_dayView_hasNextTrue() {
        // given
        User user = createUser(1L);
        Room room = createRoom(1L);
        Diary diary1 = createDiary(10L, user, room, "일기1");
        Diary diary2 = createDiary(9L, user, room, "일기2");
        Diary diary3 = createDiary(8L, user, room, "일기3"); // hasNext 판별용 extra

        given(diaryRepository.findRoomDiariesByDateRangeCursor(
                eq(user), any(LocalDate.class), any(LocalDate.class), isNull(), any()))
                .willReturn(List.of(diary1, diary2, diary3));

        // when — size=2
        DiaryCalendarListResponseDto result = diaryService.getMyDiaries(user, 2026, 5, 15, null, 2);

        // then
        assertThat(result.content()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(9L); // content 마지막 diary ID
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

    private Diary createDiary(Long id, User user, Room room, String title) {
        Diary diary = Diary.builder()
                .user(user)
                .room(room)
                .title(title)
                .contents("내용")
                .diaryDate(LocalDate.of(2026, 5, 15))
                .build();
        ReflectionTestUtils.setField(diary, "id", id);
        return diary;
    }

    private DiaryImage createDiaryImage(Long id, Diary diary, String imageKey, int sortOrder) {
        DiaryImage image = DiaryImage.builder()
                .diary(diary)
                .imageKey(imageKey)
                .sortOrder(sortOrder)
                .build();
        ReflectionTestUtils.setField(image, "id", id);
        return image;
    }
}