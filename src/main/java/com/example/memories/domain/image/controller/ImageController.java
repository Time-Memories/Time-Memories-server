package com.example.memories.domain.image.controller;

import com.example.memories.domain.image.dto.request.PresignedUrlRequestDto;
import com.example.memories.domain.image.dto.response.PresignedUrlResponseDto;
import com.example.memories.domain.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(name = "Image", description = "이미지 API")
public class ImageController {

    private final ImageService imageService;

    @Operation(summary = "업로드용 Presigned URL 발급",
            description = "S3에 직접 PUT할 presigned URL과 imageKey를 발급합니다. 여러 파일을 한 번에 요청 가능.")
    @PostMapping("/presigned")
    public ResponseEntity<PresignedUrlResponseDto> generatePresignedUrls(
            @RequestBody @Valid PresignedUrlRequestDto request
    ) {
        return ResponseEntity.ok(imageService.generatePresignedUrls(request));
    }
}
