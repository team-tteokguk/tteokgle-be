package com.advent.backend.service;

import com.advent.backend.entity.Member;
import com.advent.backend.enums.SocialProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class SocialLoginService {
    private final MemberService memberService;
    private final RestTemplate restTemplate;

    // Google OAuth 설정
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    // Kakao OAuth 설정
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    public Member login(SocialProvider provider, String code) {
        if (provider == SocialProvider.GOOGLE) {
            return loginWithGoogle(code);
        } else if (provider == SocialProvider.KAKAO) {
            return loginWithKakao(code);
        }
        throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
    }

    private Member loginWithGoogle(String code) {
        // 1. 구글 서버에 code 보내서 액세스 토큰 받기
        String googleAccessToken = getGoogleAccessToken(code);

        // 2. 액세스 토큰으로 사용자 정보 가져오기
        Map<String, Object> userInfo = getGoogleUserInfo(googleAccessToken);

        // 3. socialId 추출 (디버깅 로그 추가)
        System.out.println("🔍 구글 사용자 정보: " + userInfo);

        // ✅ 수정: "sub" 대신 "id" 사용
        String socialId = (String) userInfo.get("id");
        System.out.println("🔍 추출한 socialId: " + socialId);

        if (socialId == null || socialId.isEmpty()) {
            throw new IllegalStateException("구글에서 사용자 ID를 가져올 수 없습니다");
        }

        // 4. DB에서 회원 조회 또는 생성
        Member member = memberService.registerIFNew(socialId, Member.SocialType.GOOGLE);

        return member;
    }

    private Member loginWithKakao(String code) {
        // 1. 카카오 서버에 code 보내서 액세스 토큰 받기
        String kakaoAccessToken = getKakaoAccessToken(code);

        // 2. 액세스 토큰으로 사용자 정보 가져오기
        Map<String, Object> userInfo = getKakaoUserInfo(kakaoAccessToken);

        // 3. DB에서 회원 조회 또는 생성
        Long socialIdLong = ((Number) userInfo.get("id")).longValue();
        String socialId = String.valueOf(socialIdLong);
        Member member = memberService.registerIFNew(socialId, Member.SocialType.KAKAO);

        return member;
    }

    // ========== Google 관련 메서드 ==========

    private String getGoogleAccessToken(String code) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        return (String) response.getBody().get("access_token");
    }

    private Map<String, Object> getGoogleUserInfo(String accessToken) {
        String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, Map.class);

        return response.getBody();
    }

    // ========== Kakao 관련 메서드 ==========

    private String getKakaoAccessToken(String code) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", kakaoClientId);
        if (kakaoClientSecret != null && !kakaoClientSecret.isEmpty()) {
            params.add("client_secret", kakaoClientSecret);
        }
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        return (String) response.getBody().get("access_token");
    }

    private Map<String, Object> getKakaoUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, Map.class);

        return response.getBody();
    }
}
