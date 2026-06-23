package com.example.memories.domain.diary.dto.response;

import java.time.LocalDate;
import java.util.List;

public record CalendarCountResponseDto(
        int year,
        int month,
        List<DateCount> writtenDates
) {
    public static CalendarCountResponseDto of(int year, int month, List<DateCount> writtenDates) {
        return new CalendarCountResponseDto(year, month, writtenDates);
    }

    public record DateCount(LocalDate date, long count) {}
}
