package com.example.memories.domain.diary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DiaryCreateRequestDto(
        @NotBlank
        String title,

        @NotBlank
        String content,

        @Size(max = 5, message = "이미지는 최대 5장까지 첨부할 수 있습니다.")
        List<String> imageKeys
) {}