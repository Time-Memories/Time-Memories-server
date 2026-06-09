package com.example.memories.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatImageSendRequestDto(
        @NotEmpty(message = "이미지는 최소 1개 이상이어야 합니다.")
        @Size(max = 5, message = "이미지는 최대 5개까지 전송할 수 있습니다.")
        List<@NotBlank(message = "이미지 키는 비어 있을 수 없습니다.") String> imageKeys
) {
}
