package com.advent.backend.controller;

import com.advent.backend.dto.NotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "알림창 관리", description = "알림 목록 조회 및 읽음 처리")
@RestController
@RequestMapping("/notifications")
public class NotificationController {
    // 1. 알림 리스트 불러오기
    @Operation(summary = "알림 정보")
    @GetMapping
    public ResponseEntity<List<NotificationDto.NotificationResponse>> getAllNotifications(
            @RequestHeader("Member-Id") String userId) {
        NotificationDto.NotificationResponse notificationResponse1 =
                NotificationDto.NotificationResponse.builder()
                        .id("uuid")
                        .content("외요가 외요에게 돈을 줫습니다.")
                        .url("url")
                        .isRead(Boolean.TRUE)
                        .build();

        return ResponseEntity.ok(List.of(notificationResponse1));
    }

    // 2. 알림 전체 읽음 처리
    @Operation(summary = "알림 읽음 처리")
    @PatchMapping
    public ResponseEntity<NotificationDto.NotificationResponse> updateNotification(
            @RequestHeader("Member-Id") String userId) {
        return ResponseEntity.ok().build();
    }
}
