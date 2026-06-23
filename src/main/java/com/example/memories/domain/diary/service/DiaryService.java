package com.example.memories.domain.diary.service;

import com.example.memories.domain.diary.dto.request.DiaryCreateRequestDto;
import com.example.memories.domain.diary.dto.request.DiaryUpdateRequestDto;
import com.example.memories.domain.diary.dto.response.CalendarCountResponseDto;
import com.example.memories.domain.diary.dto.response.DiaryCalendarListResponseDto;
import com.example.memories.domain.diary.dto.response.DiaryDetailResponseDto;
import com.example.memories.domain.diary.dto.response.DiaryListResponseDto;
import com.example.memories.domain.diary.dto.response.DiaryUpdateResponseDto;
import com.example.memories.domain.user.entity.User;
import org.springframework.data.domain.Pageable;

public interface DiaryService {

    DiaryDetailResponseDto createDiary(User user, Long roomId, DiaryCreateRequestDto request);

    DiaryListResponseDto getDiaries(User user, Long roomId, Pageable pageable);

    DiaryDetailResponseDto getDiary(User user, Long diaryId);

    CalendarCountResponseDto getCalendarCounts(User user, int year, int month);

    DiaryCalendarListResponseDto getMyDiaries(User user, int year, int month, Integer day, Long cursor, int size);

    DiaryUpdateResponseDto updateDiary(User user, Long diaryId, DiaryUpdateRequestDto request);

    void deleteDiary(User user, Long diaryId);
}