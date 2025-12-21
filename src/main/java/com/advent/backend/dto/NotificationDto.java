package com.advent.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class NotificationDto {
    // [응답] 알림 조회
    @Getter
    @Builder
    @AllArgsConstructor
    public static class NotificationResponse {
        @Schema(description = "알림 ID")
        private String id;

        @Schema(description = "알림 내용")
        private String content;

        @Schema(description = "알림 URL")
        private String url;

        @Schema(description = "알림 읽음 여부")
        private Boolean isRead;

        @Schema(description = "알림 생성 시간")
        private LocalDateTime date;
    }
}
