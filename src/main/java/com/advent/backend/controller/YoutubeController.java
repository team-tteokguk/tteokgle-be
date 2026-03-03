package com.advent.backend.controller;

import com.advent.backend.dto.YoutubeDto;
import com.advent.backend.service.YoutubeValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "유튜브", description = "유튜브 링크 유효성 검사")
@RestController
@RequestMapping("/youtube")
@RequiredArgsConstructor
public class YoutubeController {

    private final YoutubeValidationService youtubeValidationService;

    @Operation(summary = "유튜브 임베드 가능 여부 검사")
    @PostMapping("/embed/validate")
    public ResponseEntity<YoutubeDto.EmbedValidationResponse> validateEmbed(
            @RequestBody YoutubeDto.EmbedValidationRequest request) {
        String input = request == null ? null : request.getUrl();
        return ResponseEntity.ok(youtubeValidationService.validateEmbeddable(input));
    }
}
