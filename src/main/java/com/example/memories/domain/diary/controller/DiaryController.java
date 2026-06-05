package com.example.memories.domain.diary.controller;

import com.example.memories.domain.diary.dto.request.DiaryCreateRequestDto;
import com.example.memories.domain.diary.dto.request.DiaryUpdateRequestDto;
import com.example.memories.domain.diary.dto.response.CalendarCountResponseDto;
import com.example.memories.domain.diary.dto.response.DiaryCalendarListResponseDto;
import com.example.memories.domain.diary.dto.response.DiaryDetailResponseDto;
import com.example.memories.domain.diary.dto.response.DiaryListResponseDto;
import com.example.memories.domain.diary.dto.response.DiaryUpdateResponseDto;
import com.example.memories.domain.diary.service.DiaryService;
import com.example.memories.domain.user.entity.User;
import com.example.memories.global.annotation.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "Diary", description = "일기 API")
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(summary = "일기 생성", description = "방 멤버가 일기를 작성합니다. imageKeys 배열 순서가 이미지 표시 순서로 저장됩니다.")
    @PostMapping("/api/rooms/{roomId}/diaries")
    public ResponseEntity<DiaryDetailResponseDto> createDiary(
            @CurrentUser User user,
            @PathVariable Long roomId,
            @RequestBody @Valid DiaryCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diaryService.createDiary(user, roomId, request));
    }

    @Operation(summary = "일기 목록 조회", description = "방의 일기 목록을 최신순으로 조회합니다. 대표 이미지 1장(thumbnailUrl)만 포함됩니다.")
    @GetMapping("/api/rooms/{roomId}/diaries")
    public ResponseEntity<DiaryListResponseDto> getDiaries(
            @CurrentUser User user,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(diaryService.getDiaries(user, roomId, PageRequest.of(page, size)));
    }

    @Operation(summary = "일기 상세 조회", description = "일기 내용과 전체 이미지를 조회합니다. 해당 일기가 속한 방의 멤버만 조회 가능합니다.")
    @GetMapping("/api/diaries/{diaryId}")
    public ResponseEntity<DiaryDetailResponseDto> getDiary(
            @CurrentUser User user,
            @PathVariable Long diaryId
    ) {
        return ResponseEntity.ok(diaryService.getDiary(user, diaryId));
    }

    @Operation(summary = "월별 일기 작성 현황 조회", description = "달력 마커 표시용으로, 사용자가 속한 모든 방의 날짜별 일기 개수를 반환합니다.")
    @GetMapping("/api/diaries/calendar/counts")
    public ResponseEntity<CalendarCountResponseDto> getCalendarCounts(
            @CurrentUser User user,
            @RequestParam @Min(1900) int year,
            @RequestParam @Min(1) @Max(12) int month
    ) {
        return ResponseEntity.ok(diaryService.getCalendarCounts(user, year, month));
    }

    @Operation(summary = "소속 방 일기 모아보기", description = "사용자가 속한 방의 일기를 월 또는 특정 일 기준으로 커서 기반 조회합니다. day 미입력 시 해당 월 전체를 반환합니다.")
    @GetMapping("/api/diaries/my-all")
    public ResponseEntity<DiaryCalendarListResponseDto> getMyDiaries(
            @CurrentUser User user,
            @RequestParam @Min(1900) int year,
            @RequestParam @Min(1) @Max(12) int month,
            @RequestParam(required = false) @Min(1) @Max(31) Integer day,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(diaryService.getMyDiaries(user, year, month, day, cursor, size));
    }

    @Operation(summary = "일기 수정", description = "본인이 작성한 일기를 수정합니다.")
    @PutMapping("/api/diaries/{diaryId}")
    public ResponseEntity<DiaryUpdateResponseDto> updateDiary(
            @CurrentUser User user,
            @PathVariable Long diaryId,
            @RequestBody @Valid DiaryUpdateRequestDto request
    ) {
        return ResponseEntity.ok(diaryService.updateDiary(user, diaryId, request));
    }

    @Operation(summary = "일기 삭제", description = "본인이 작성한 일기를 삭제합니다.")
    @DeleteMapping("/api/diaries/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @CurrentUser User user,
            @PathVariable Long diaryId
    ) {
        diaryService.deleteDiary(user, diaryId);
        return ResponseEntity.noContent().build();
    }
}