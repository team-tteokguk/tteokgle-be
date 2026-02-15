package com.advent.backend.service;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.UploadDto;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class UploadService {
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket:}")
    private String bucket;

    @Value("${aws.s3.region:ap-northeast-2}")
    private String region;

    @Value("${aws.s3.presign-expiration-seconds:300}")
    private long presignExpirationSeconds;

    @Value("${aws.s3.public-base-url:}")
    private String publicBaseUrl;

    public UploadDto.PresignResponse createImagePresignedUrl(
            UUID memberId, UploadDto.PresignRequest request) {
        validateRequest(request);
        validateBucketConfigured();

        String extension = extractExtension(request.getFileName());
        String key =
                String.format(
                        "items/%s/%s/%s%s",
                        memberId, LocalDate.now(), UUID.randomUUID(), extension);

        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(request.getContentType())
                        .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(presignExpirationSeconds))
                        .putObjectRequest(putObjectRequest)
                        .build();

        PresignedPutObjectRequest presignedRequest;
        try {
            presignedRequest = s3Presigner.presignPutObject(presignRequest);
        } catch (S3Exception e) {
            throw new IllegalStateException("S3 presign 생성에 실패했습니다.", e);
        }

        return UploadDto.PresignResponse.builder()
                .uploadUrl(presignedRequest.url().toString())
                .key(key)
                .fileUrl(buildPublicFileUrl(key))
                .expiresIn(presignExpirationSeconds)
                .method("PUT")
                .build();
    }

    private void validateRequest(UploadDto.PresignRequest request) {
        if (request == null
                || request.getFileName() == null
                || request.getFileName().isBlank()
                || request.getContentType() == null
                || request.getContentType().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (!request.getContentType().startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateBucketConfigured() {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("aws.s3.bucket 설정이 필요합니다.");
        }
    }

    private String buildPublicFileUrl(String key) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.endsWith("/") ? publicBaseUrl + key : publicBaseUrl + "/" + key;
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex);
    }
}
