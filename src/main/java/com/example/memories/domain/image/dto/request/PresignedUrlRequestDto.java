package com.example.memories.domain.image.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PresignedUrlRequestDto(
        @NotEmpty
        @Size(max = 5, message = "한번에 최대 5개의 이미지만 업로드할 수 있습니다.")
        List<@Valid FileRequest> files
) {
    public record FileRequest(
            @NotBlank
            String fileName,

            @NotBlank
            @Pattern(
                    regexp = "image/(jpeg|png|webp|gif|heic)",
                    message = "허용되지 않는 Content-Type입니다. (jpeg, png, webp, gif, heic만 가능)"
            )
            String contentType
    ) {}
}
