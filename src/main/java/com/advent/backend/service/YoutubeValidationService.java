package com.advent.backend.service;

import com.advent.backend.dto.YoutubeDto;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class YoutubeValidationService {

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private static final String OEMBED_URL = "https://www.youtube.com/oembed?url=%s&format=json";
    private static final Set<String> YOUTUBE_HOSTS =
            Set.of("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com");
    private static final Set<String> YOUTU_BE_HOSTS = Set.of("youtu.be", "www.youtu.be");
    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public YoutubeDto.EmbedValidationResponse validateEmbeddable(String input) {
        String normalized = normalize(input);
        if (normalized == null) {
            return invalid("url이 비어 있습니다.");
        }

        // 영상 ID만 전달된 경우도 허용
        if (isValidVideoId(normalized)) {
            return checkEmbeddableByOembed(normalized);
        }

        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (Exception e) {
            return invalid("유효한 URL 형식이 아닙니다.");
        }

        String host = normalizeHost(uri.getHost());
        if (host == null) {
            return invalid("URL 호스트가 없습니다.");
        }

        String videoId = extractVideoId(uri, host);
        if (!isValidVideoId(videoId)) {
            return invalid("유튜브 영상 ID를 찾을 수 없습니다.");
        }

        return checkEmbeddableByOembed(videoId);
    }

    private String extractVideoId(URI uri, String host) {
        String path = uri.getPath() == null ? "" : uri.getPath();

        if (YOUTU_BE_HOSTS.contains(host)) {
            return firstPathSegment(path);
        }

        if (!YOUTUBE_HOSTS.contains(host)) {
            return null;
        }

        if ("/watch".equals(path)) {
            return queryParam(uri.getQuery(), "v");
        }

        if (path.startsWith("/embed/")) {
            return firstPathSegment(path.substring("/embed/".length()));
        }

        if (path.startsWith("/shorts/")) {
            return firstPathSegment(path.substring("/shorts/".length()));
        }

        if (path.startsWith("/live/")) {
            return firstPathSegment(path.substring("/live/".length()));
        }

        return null;
    }

    private String queryParam(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    private String firstPathSegment(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String cleaned = path.startsWith("/") ? path.substring(1) : path;
        int slash = cleaned.indexOf('/');
        return slash >= 0 ? cleaned.substring(0, slash) : cleaned;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeHost(String host) {
        if (host == null) {
            return null;
        }
        String normalized = host.trim().toLowerCase();
        return normalized.startsWith("www.") ? normalized : normalized;
    }

    private boolean isValidVideoId(String value) {
        return value != null && VIDEO_ID_PATTERN.matcher(value).matches();
    }

    private YoutubeDto.EmbedValidationResponse valid(String videoId) {
        return YoutubeDto.EmbedValidationResponse.builder()
                .embeddable(true)
                .videoId(videoId)
                .embedUrl("https://www.youtube.com/embed/" + videoId)
                .reason(null)
                .build();
    }

    private YoutubeDto.EmbedValidationResponse checkEmbeddableByOembed(String videoId) {
        try {
            String watchUrl = "https://www.youtube.com/watch?v=" + videoId;
            String encodedWatchUrl = URLEncoder.encode(watchUrl, StandardCharsets.UTF_8);
            String endpoint = OEMBED_URL.formatted(encodedWatchUrl);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .timeout(Duration.ofSeconds(3))
                            .GET()
                            .build();

            HttpResponse<Void> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 200) {
                return valid(videoId);
            }
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                return invalid("동영상 소유자가 외부 임베드를 허용하지 않았습니다.");
            }
            if (response.statusCode() == 404) {
                return invalid("존재하지 않거나 비공개된 영상입니다.");
            }
            return invalid("유튜브 임베드 검증 실패(status=" + response.statusCode() + ")");
        } catch (Exception e) {
            return invalid("유튜브 임베드 검증 요청에 실패했습니다.");
        }
    }

    private YoutubeDto.EmbedValidationResponse invalid(String reason) {
        return YoutubeDto.EmbedValidationResponse.builder()
                .embeddable(false)
                .videoId(null)
                .embedUrl(null)
                .reason(reason)
                .build();
    }
}
