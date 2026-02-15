package com.advent.backend.controller;

import com.advent.backend.dto.UploadDto;
import com.advent.backend.security.CustomUserDetails;
import com.advent.backend.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "파일 업로드", description = "이미지 업로드 presigned URL 발급 API")
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class UploadController {
    private final UploadService uploadService;

    @Operation(summary = "이미지 업로드 Presigned URL 발급")
    @PostMapping("/presign")
    public ResponseEntity<UploadDto.PresignResponse> createPresignedUrl(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody UploadDto.PresignRequest request) {
        return ResponseEntity.ok(
                uploadService.createImagePresignedUrl(
                        customUserDetails.getMember().getId(), request));
    }
}
