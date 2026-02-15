package com.advent.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UploadDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PresignRequest {
        @Schema(description = "원본 파일명", example = "garnish.png")
        private String fileName;

        @Schema(description = "MIME 타입", example = "image/png")
        private String contentType;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PresignResponse {
        @Schema(description = "S3 업로드용 Presigned URL (PUT)")
        private String uploadUrl;

        @Schema(description = "업로드된 파일 key")
        private String key;

        @Schema(description = "최종 파일 접근 URL")
        private String fileUrl;

        @Schema(description = "URL 만료(초)")
        private long expiresIn;

        @Schema(description = "업로드 HTTP 메서드")
        private String method;
    }
}
