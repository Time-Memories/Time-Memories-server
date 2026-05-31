package com.example.memories.domain.image.service;

import com.example.memories.domain.image.dto.request.PresignedUrlRequestDto;
import com.example.memories.domain.image.dto.response.PresignedUrlResponseDto;

public interface ImageService {
    PresignedUrlResponseDto generatePresignedUrls(PresignedUrlRequestDto request);
    void deleteImage(String imageKey);
}
