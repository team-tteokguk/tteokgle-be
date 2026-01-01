package com.advent.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class GuestBookDto {
    // 1. [응답] 방명록 조회
    @Getter
    @Builder
    @AllArgsConstructor
    public static class GuestBookResponse {
        @Schema(description = "방명록 ID")
        private String id;

        @Schema(description = "작성자 ID")
        private String writerId;

        @Schema(description = "작성자 닉네임")
        private String writerNickname;

        @Schema(description = "작성자 프로필 사진")
        private String writerImageUrl;

        @Schema(description = "방명록 내용")
        private String content;

        @Schema(description = "방명록 작성 날짜 및 시간")
        private LocalDateTime createdAt;
    }

    // 2. [요청] 방명록 등록
    @Getter
    @NoArgsConstructor
    public static class GuestBookRequest {
        @Schema(description = "방명록 내용")
        private String content;
    }
}
