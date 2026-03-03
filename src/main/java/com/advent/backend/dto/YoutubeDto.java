package com.advent.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class YoutubeDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "유튜브 임베드 유효성 검사 요청")
    public static class EmbedValidationRequest {
        @Schema(description = "유튜브 URL 또는 영상 ID", example = "https://youtu.be/dQw4w9WgXcQ")
        private String url;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "유튜브 임베드 유효성 검사 응답")
    public static class EmbedValidationResponse {
        @Schema(description = "임베드 가능 여부")
        private boolean embeddable;

        @Schema(description = "유효한 경우 추출된 영상 ID")
        private String videoId;

        @Schema(description = "유효한 경우 변환된 임베드 URL")
        private String embedUrl;

        @Schema(description = "유효성 실패 사유")
        private String reason;
    }
}
