package com.advent.backend.security;

import com.advent.backend.entity.Member;

public interface OAuth2UserInfo {
    Member.SocialType getSocialType();
    String getSocialId();
    String getNickname();
}
