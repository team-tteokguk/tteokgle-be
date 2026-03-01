package com.advent.backend.dto;

import com.advent.backend.entity.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

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

        public static NotificationResponse from(Notification notification) {
            return NotificationResponse.builder()
                    .id(notification.getId() == null ? null : notification.getId().toString())
                    .content(notification.getMessage())
                    .url(notification.getLink())
                    .isRead(notification.isRead())
                    .date(notification.getCreatedAt())
                    .build();
        }
    }
}
