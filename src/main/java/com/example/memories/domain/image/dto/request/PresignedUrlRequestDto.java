package com.example.memories.domain.image.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record PresignedUrlRequestDto(
        @NotEmpty List<@Valid FileRequest> files
) {
    public record FileRequest(
            @NotBlank String fileName,
            @NotBlank
            @Pattern(
                    regexp = "image/(jpeg|png|webp|gif|heic)",
                    message = "허용되지 않는 Content-Type입니다. (jpeg, png, webp, gif, heic만 가능)"
            )
            String contentType
    ) {}
}
