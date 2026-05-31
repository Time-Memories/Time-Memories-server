package com.example.memories.infra.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class S3PresignServiceTest {

    @Mock S3Presigner s3Presigner;
    @Mock S3Client s3Client;
    @Mock PresignedPutObjectRequest presignedPutObjectRequest;
    @InjectMocks S3PresignService s3PresignService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3PresignService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(s3PresignService, "keyPrefix", "dev");
        ReflectionTestUtils.setField(s3PresignService, "cloudfrontDomain", "test.cloudfront.net");
    }

    @Test
    @DisplayName("presign() - presigned URL, imageKey, CloudFront URL, 유효시간을 올바르게 반환한다")
    void presign_returnsPresignResult() {
        stubPresigner();

        S3PresignService.PresignResult result = s3PresignService.presign("photo.jpg", "image/jpeg");

        assertThat(result.presignedUrl()).isEqualTo("https://s3.amazonaws.com/presigned-url");
        assertThat(result.imageKey()).matches("dev/images/\\d{4}/\\d{2}/[a-f0-9\\-]+\\.jpg");
        assertThat(result.imageUrl()).isEqualTo("https://test.cloudfront.net/" + result.imageKey());
        assertThat(result.expiresIn()).isEqualTo(S3PresignService.EXPIRY_SECONDS);
    }

    @Test
    @DisplayName("presign() - 확장자가 없는 파일명은 imageKey에 확장자 없이 생성된다")
    void presign_noExtension_imageKeyHasNoExtension() {
        stubPresigner();

        S3PresignService.PresignResult result = s3PresignService.presign("photo", "image/jpeg");

        assertThat(result.imageKey()).matches("dev/images/\\d{4}/\\d{2}/[a-f0-9\\-]+");
        assertThat(result.imageKey()).doesNotContain(".");
    }

    @Test
    @DisplayName("presign() - 대문자 확장자는 소문자로 정규화된다")
    void presign_uppercaseExtension_normalizedToLowercase() {
        stubPresigner();

        S3PresignService.PresignResult result = s3PresignService.presign("photo.JPG", "image/jpeg");

        assertThat(result.imageKey()).endsWith(".jpg");
    }

    @Test
    @DisplayName("presign() - 파일명 끝에 점만 있으면 확장자 없이 처리된다")
    void presign_trailingDot_noExtension() {
        stubPresigner();

        S3PresignService.PresignResult result = s3PresignService.presign("photo.", "image/jpeg");

        assertThat(result.imageKey()).matches("dev/images/\\d{4}/\\d{2}/[a-f0-9\\-]+");
        assertThat(result.imageKey()).doesNotContain(".");
    }

    @Test
    @DisplayName("resolveImageUrl() - CloudFront 도메인으로 https URL을 생성한다")
    void resolveImageUrl_buildsHttpsUrl() {
        String url = s3PresignService.resolveImageUrl("dev/images/2024/01/uuid.jpg");

        assertThat(url).isEqualTo("https://test.cloudfront.net/dev/images/2024/01/uuid.jpg");
    }

    @Test
    @DisplayName("delete() - imageKey를 S3Client 삭제 요청으로 전달한다")
    @SuppressWarnings("unchecked")
    void delete_delegatesToS3Client() {
        s3PresignService.delete("dev/images/2024/01/uuid.jpg");

        then(s3Client).should().deleteObject(any(Consumer.class));
    }

    private void stubPresigner() {
        given(presignedPutObjectRequest.url()).willReturn(makeUrl("https://s3.amazonaws.com/presigned-url"));
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedPutObjectRequest);
    }

    private URL makeUrl(String url) {
        try {
            return URI.create(url).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
