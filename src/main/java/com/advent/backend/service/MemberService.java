package com.advent.backend.service;

import com.advent.backend.entity.Member;
import com.advent.backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    // 닉네임 수정
    public void updateNickName(String nickname) {}

    // 닉네임 유효성 확인
    public void checkNickname(String nickname) {
        // 정규방정식으로 확인
        // 중복 확인

        // 글자 수 확인

        // 비방용, 외설적 닉네임인지 확인
    }

    // 회원 등록
    public Member registerIFNew(String socialId, Member.SocialType socialType) {
        return memberRepository
                .findBySocialId(socialId)
                .map(
                        existedMember -> {
                            return existedMember;
                        })
                .orElseGet(
                        () -> {
                            return memberRepository.save(
                                    Member.builder()
                                            .socialId(socialId)
                                            .socialType(socialType)
                                            .build());
                        });
    }
}
