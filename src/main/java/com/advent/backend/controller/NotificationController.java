package com.advent.backend.controller;

import com.advent.backend.dto.NotificationDto;
import com.advent.backend.security.CustomUserDetails;
import com.advent.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "알림창 관리", description = "알림 목록 조회 및 읽음 처리")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    final NotificationService notificationService;

    // 1. 알림 리스트 불러오기
    @Operation(summary = "알림 정보")
    @GetMapping
    public ResponseEntity<Slice<NotificationDto.NotificationResponse>> getAllNotifications(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PageableDefault(size = 50) Pageable pageable) {
        String memberId = customUserDetails.getMember().getId().toString();
        log.info(
                "[알림] 목록 조회 요청: memberId={}, page={}, size={}",
                memberId,
                pageable.getPageNumber(),
                pageable.getPageSize());
        return ResponseEntity.ok()
                .header("X-Debug-Member-Id", memberId)
                .body(
                        notificationService.getNotifications(
                                customUserDetails.getMember().getId(), pageable));
    }

    @Operation(summary = "알림 SSE 구독")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeNotification(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        log.info("[SSE] 구독 요청: memberId={}", customUserDetails.getMember().getId());
        return notificationService.subscribe(customUserDetails.getMember().getId());
    }

    // 2. 알림 전체 읽음 처리
    @Operation(summary = "알림 읽음 처리")
    @PatchMapping
    public ResponseEntity<Void> updateNotification(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        notificationService.readNotification(customUserDetails.getMember().getId());
        return ResponseEntity.noContent().build();
    }
}
