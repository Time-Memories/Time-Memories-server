package com.example.memories.infra.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3PresignService {

    static final int EXPIRY_SECONDS = 300;

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.key-prefix}")
    private String keyPrefix;

    @Value("${aws.cloudfront.domain}")
    private String cloudfrontDomain;

    public PresignResult presign(String fileName, String contentType) {
        String imageKey = buildImageKey(fileName);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(imageKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(EXPIRY_SECONDS))
                .putObjectRequest(putRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        return new PresignResult(imageKey, presigned.url().toString(), resolveImageUrl(imageKey), EXPIRY_SECONDS);
    }

    public String resolveImageUrl(String imageKey) {
        return "https://" + cloudfrontDomain + "/" + imageKey;
    }

    public void delete(String imageKey) {
        s3Client.deleteObject(req -> req.bucket(bucket).key(imageKey));
    }

    // 원본 파일명에서 확장자만 추출하고 UUID를 파일명으로 사용해 URL 인코딩 문제를 방지
    private String buildImageKey(String fileName) {
        LocalDate today = LocalDate.now();
        String extension = extractExtension(fileName);
        return String.format("%s/images/%d/%02d/%s%s",
                keyPrefix, today.getYear(), today.getMonthValue(), UUID.randomUUID(), extension);
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(dot + 1).toLowerCase().replaceAll("[^a-z0-9]", "");
        return ext.isEmpty() ? "" : "." + ext;
    }

    public record PresignResult(String imageKey, String presignedUrl, String imageUrl, int expiresIn) {}
}
