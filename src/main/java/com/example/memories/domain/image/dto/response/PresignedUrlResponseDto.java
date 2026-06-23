package com.example.memories.domain.image.dto.response;

import java.util.List;

public record PresignedUrlResponseDto(List<UploadInfo> uploads) {
    public record UploadInfo(
            String imageKey,
            String presignedUrl,
            String imageUrl,
            int expiresIn
    ) {}
}
