package com.advent.backend.security;

import com.advent.backend.entity.Member;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class KakaoUserDetails implements OAuth2UserInfo {
    private Map<String, Object> attributes;

    @Override
    public Member.SocialType getSocialType() {
        return Member.SocialType.KAKAO;
    }

    @Override
    public String getSocialId() {
        return attributes.get("id").toString();
    }

    @Override
    public String getNickname() {
        return (String) ((Map) attributes.get("properties")).get("nickname");
    }
}
