package com.advent.backend.security;

import com.advent.backend.entity.Member;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GoogleUserDetails implements OAuth2UserInfo {

    private Map<String, Object> attributes;

    @Override
    public Member.SocialType getSocialType() {
        return Member.SocialType.GOOGLE;
    }

    @Override
    public String getSocialId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getNickname() {
        return (String) attributes.get("name");
    }
}
