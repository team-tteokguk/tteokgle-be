package com.advent.backend.service;

import com.advent.backend.entity.Member;
import com.advent.backend.repository.MemberRepository;
import com.advent.backend.security.KakaoUserDetails;
import com.advent.backend.security.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // service 식별자 이름 ("KAKAO, GOOGLE 등")

        OAuth2UserInfo userInfo = null;
        if (registrationId.equals("kakao")) {
            userInfo = new KakaoUserDetails(oAuth2User.getAttributes());
        }

        saveOrUpdate(userInfo);

        return oAuth2User;
    }

    private void saveOrUpdate(OAuth2UserInfo userInfo) {
        memberRepository.findBySocialTypeAndSocialId(userInfo.getSocialType(), userInfo.getSocialId())
                .map(member -> member.updateNickname(userInfo.getNickname())) // 있으면 업데이트
                .orElseGet(() -> memberRepository.save(Member.builder() // 없으면 생성
                        .socialType(userInfo.getSocialType())
                        .socialId(userInfo.getSocialId())
                        .nickname(userInfo.getNickname())
                        .build()));

    }
}
