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
import com.example.memories.domain.room.exception.RoomErrorCode;
import com.example.memories.domain.room.repository.RoomRepository;
import com.example.memories.domain.room.repository.RoomUserRepository;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.exception.BusinessException;
import com.example.memories.infra.s3.S3ImageDeleteEvent;
import com.example.memories.infra.s3.S3PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final DiaryImageRepository diaryImageRepository;
    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final S3PresignService s3PresignService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public DiaryDetailResponseDto createDiary(User user, Long roomId, DiaryCreateRequestDto request) {
        Room room = findRoomOrThrow(roomId);
        validateRoomMember(room, user);

        Diary diary = Diary.builder()
                .user(user)
                .room(room)
                .title(request.title())
                .contents(request.content())
                .diaryDate(request.diaryDate())
                .build();
        diaryRepository.save(diary);

        List<String> imageKeys = request.imageKeys() != null ? request.imageKeys() : Collections.emptyList();
        diary.updateImages(imageKeys);

        return DiaryDetailResponseDto.of(diary, resolveImageUrls(imageKeys));
    }

    @Override
    public DiaryListResponseDto getDiaries(User user, Long roomId, Pageable pageable) {
        Room room = findRoomOrThrow(roomId);
        validateRoomMember(room, user);

        Page<Diary> diaryPage = diaryRepository.findAllByRoomWithUser(room, pageable);
        List<Diary> diaries = diaryPage.getContent();

        if (diaries.isEmpty()) {
            return DiaryListResponseDto.of(diaryPage, Collections.emptyList());
        }

        // 대표 이미지 배치 조회 후 diaryId → thumbnailUrl 맵 구성
        Map<Long, String> thumbnailMap = diaryImageRepository.findThumbnailsByDiaries(diaries)
                .stream()
                .collect(Collectors.toMap(
                        di -> di.getDiary().getId(),
                        di -> s3PresignService.resolveImageUrl(di.getImageKey())
                ));

        List<DiaryListResponseDto.DiaryDto> content = diaries.stream()
                .map(d -> DiaryListResponseDto.DiaryDto.of(d, thumbnailMap.get(d.getId())))
                .toList();

        return DiaryListResponseDto.of(diaryPage, content);
    }

    @Override
    public DiaryDetailResponseDto getDiary(User user, Long diaryId) {
        Diary diary = findDiaryWithImagesOrThrow(diaryId);
        validateRoomMember(diary.getRoom(), user);

        return DiaryDetailResponseDto.of(diary, resolveImageUrlsFromEntities(diary.getImages()));
    }

    @Override
    public CalendarCountResponseDto getCalendarCounts(User user, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<CalendarCountResponseDto.DateCount> writtenDates = diaryRepository
                .countDiariesByDateRange(user, startDate, endDate)
                .stream()
                .map(row -> new CalendarCountResponseDto.DateCount(
                        (LocalDate) row[0],
                        (Long) row[1]
                ))
                .toList();

        return CalendarCountResponseDto.of(year, month, writtenDates);
    }

    @Override
    public DiaryCalendarListResponseDto getMyDiaries(User user, int year, int month, Integer day, Long cursor, int size) {
        LocalDate startDate;
        LocalDate endDate;
        if (day != null) {
            startDate = LocalDate.of(year, month, day);
            endDate = startDate;
        } else {
            startDate = LocalDate.of(year, month, 1);
            endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        }

        List<Diary> fetched = diaryRepository.findRoomDiariesByDateRangeCursor(
                user, startDate, endDate, cursor, PageRequest.of(0, size + 1));

        boolean hasNext = fetched.size() > size;
        List<Diary> content = hasNext ? fetched.subList(0, size) : fetched;

        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        List<DiaryCalendarListResponseDto.DiaryItem> items = content.stream()
                .map(DiaryCalendarListResponseDto.DiaryItem::from)
                .toList();

        return DiaryCalendarListResponseDto.of(items, nextCursor, hasNext);
    }

    @Transactional
    @Override
    public DiaryUpdateResponseDto updateDiary(User user, Long diaryId, DiaryUpdateRequestDto request) {
        Diary diary = findDiaryWithImagesOrThrow(diaryId);

        if (!diary.isAuthor(user.getId())) {
            throw new BusinessException(DiaryErrorCode.DIARY_NOT_AUTHOR);
        }

        diary.update(request.title(), request.content(), request.diaryDate());

        List<String> newImageKeys = request.imageKeys() != null ? request.imageKeys() : Collections.emptyList();

        // 새 목록에 없는 기존 키 수집 (updateImages 호출 전에 수집해야 함)
        Set<String> newKeySet = new HashSet<>(newImageKeys);
        List<String> keysToDelete = diary.getImages().stream()
                .map(DiaryImage::getImageKey)
                .filter(key -> !newKeySet.contains(key))
                .toList();

        // orphanRemoval로 기존 이미지 컬렉션 교체
        diary.updateImages(newImageKeys);

        // DB 커밋 성공 후 S3 삭제 (트랜잭션 롤백 시 S3 삭제 방지)
        if (!keysToDelete.isEmpty()) {
            eventPublisher.publishEvent(new S3ImageDeleteEvent(keysToDelete));
        }

        return DiaryUpdateResponseDto.of(diary, resolveImageUrls(newImageKeys));
    }

    @Transactional
    @Override
    public void deleteDiary(User user, Long diaryId) {
        Diary diary = findDiaryWithImagesOrThrow(diaryId);

        if (!diary.isAuthor(user.getId())) {
            throw new BusinessException(DiaryErrorCode.DIARY_NOT_AUTHOR);
        }

        // DB 커밋 성공 후 S3 삭제 (트랜잭션 롤백 시 S3 삭제 방지)
        List<String> keysToDelete = diary.getImages().stream()
                .map(DiaryImage::getImageKey)
                .toList();
        if (!keysToDelete.isEmpty()) {
            eventPublisher.publishEvent(new S3ImageDeleteEvent(keysToDelete));
        }

        diaryRepository.delete(diary);
    }

    private Room findRoomOrThrow(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
    }

    private Diary findDiaryWithImagesOrThrow(Long diaryId) {
        return diaryRepository.findWithImagesById(diaryId)
                .orElseThrow(() -> new BusinessException(DiaryErrorCode.DIARY_NOT_FOUND));
    }

    private void validateRoomMember(Room room, User user) {
        if (!roomUserRepository.existsByRoomAndUser(room, user)) {
            throw new BusinessException(DiaryErrorCode.DIARY_FORBIDDEN);
        }
    }

    private List<String> resolveImageUrls(List<String> imageKeys) {
        return imageKeys.stream()
                .map(s3PresignService::resolveImageUrl)
                .toList();
    }

    private List<String> resolveImageUrlsFromEntities(List<DiaryImage> images) {
        return images.stream()
                .map(di -> s3PresignService.resolveImageUrl(di.getImageKey()))
                .toList();
    }
}