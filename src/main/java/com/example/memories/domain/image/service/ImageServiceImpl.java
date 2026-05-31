package com.example.memories.domain.image.service;

import com.example.memories.domain.image.dto.request.PresignedUrlRequestDto;
import com.example.memories.domain.image.dto.response.PresignedUrlResponseDto;
import com.example.memories.infra.s3.S3PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final S3PresignService s3PresignService;

    @Override
    public PresignedUrlResponseDto generatePresignedUrls(PresignedUrlRequestDto request) {
        List<PresignedUrlResponseDto.UploadInfo> uploads = request.files().stream()
                .map(file -> {
                    S3PresignService.PresignResult result =
                            s3PresignService.presign(file.fileName(), file.contentType());
                    return new PresignedUrlResponseDto.UploadInfo(
                            result.imageKey(), result.presignedUrl(), result.imageUrl(), result.expiresIn());
                })
                .toList();

        return new PresignedUrlResponseDto(uploads);
    }

    @Override
    public void deleteImage(String imageKey) {
        s3PresignService.delete(imageKey);
    }
}
