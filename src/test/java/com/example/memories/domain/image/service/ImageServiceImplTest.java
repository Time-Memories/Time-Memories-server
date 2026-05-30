package com.example.memories.domain.image.service;

import com.example.memories.domain.image.dto.request.PresignedUrlRequestDto;
import com.example.memories.domain.image.dto.response.PresignedUrlResponseDto;
import com.example.memories.infra.s3.S3PresignService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {

    @Mock S3PresignService s3PresignService;
    @InjectMocks ImageServiceImpl imageService;

    @Test
    @DisplayName("generatePresignedUrls() - 단일 파일 요청 시 presigned URL 정보를 반환한다")
    void generatePresignedUrls_singleFile_returnsUploadInfo() {
        given(s3PresignService.presign("photo.jpg", "image/jpeg"))
                .willReturn(new S3PresignService.PresignResult(
                        "dev/images/2024/01/uuid.jpg",
                        "https://s3.amazonaws.com/presigned",
                        "https://test.cloudfront.net/dev/images/2024/01/uuid.jpg",
                        300));

        PresignedUrlRequestDto request = new PresignedUrlRequestDto(List.of(
                new PresignedUrlRequestDto.FileRequest("photo.jpg", "image/jpeg")
        ));

        PresignedUrlResponseDto result = imageService.generatePresignedUrls(request);

        assertThat(result.uploads()).hasSize(1);
        PresignedUrlResponseDto.UploadInfo info = result.uploads().get(0);
        assertThat(info.imageKey()).isEqualTo("dev/images/2024/01/uuid.jpg");
        assertThat(info.presignedUrl()).isEqualTo("https://s3.amazonaws.com/presigned");
        assertThat(info.imageUrl()).isEqualTo("https://test.cloudfront.net/dev/images/2024/01/uuid.jpg");
        assertThat(info.expiresIn()).isEqualTo(300);
    }

    @Test
    @DisplayName("generatePresignedUrls() - 여러 파일 요청 시 파일별로 각각 presign을 호출한다")
    void generatePresignedUrls_multipleFiles_presignsEachFile() {
        given(s3PresignService.presign("a.jpg", "image/jpeg"))
                .willReturn(new S3PresignService.PresignResult("key-a", "https://presigned-a", "https://cf/key-a", 300));
        given(s3PresignService.presign("b.png", "image/png"))
                .willReturn(new S3PresignService.PresignResult("key-b", "https://presigned-b", "https://cf/key-b", 300));

        PresignedUrlRequestDto request = new PresignedUrlRequestDto(List.of(
                new PresignedUrlRequestDto.FileRequest("a.jpg", "image/jpeg"),
                new PresignedUrlRequestDto.FileRequest("b.png", "image/png")
        ));

        PresignedUrlResponseDto result = imageService.generatePresignedUrls(request);

        assertThat(result.uploads()).hasSize(2);
        assertThat(result.uploads().get(0).imageKey()).isEqualTo("key-a");
        assertThat(result.uploads().get(1).imageKey()).isEqualTo("key-b");
        then(s3PresignService).should(times(2)).presign(anyString(), anyString());
    }

    @Test
    @DisplayName("deleteImage() - imageKey를 S3PresignService.delete()로 전달한다")
    void deleteImage_delegatesToS3PresignService() {
        imageService.deleteImage("dev/images/2024/01/uuid.jpg");

        then(s3PresignService).should().delete("dev/images/2024/01/uuid.jpg");
    }
}
