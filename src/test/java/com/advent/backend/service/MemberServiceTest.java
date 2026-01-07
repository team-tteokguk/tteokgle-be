package com.advent.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.entity.Member;
import com.advent.backend.repository.MemberRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MemberServiceTest {
    @Autowired MemberService memberService;

    @Autowired MemberRepository memberRepository;

    private Member memberA;

    @BeforeEach
    public void init() {
        memberA =
                Member.builder()
                        .nickname("있는이름")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("buyer_id")
                        .point(100)
                        .build();

        memberRepository.save(memberA);
    }

    @Test
    @DisplayName("닉네임 업데이트 테스트를 진행합니다.")
    public void should_ChangeNickName() {
        String newNickName = "새 닉네임";

        memberService.updateNickName(memberA.getId(), newNickName);

        Member newNickNameMember = memberRepository.findById(memberA.getId()).orElseThrow();

        assertThat(newNickNameMember.getNickname()).isEqualTo(newNickName);
    }

    @Test
    @DisplayName("신규 소셜 ID로 가입하면 새로운 회원이 생성되어야 한다.")
    public void should_ReturnNew_when_Register() {
        String socialId = "socialId";
        Member.SocialType socialType = Member.SocialType.KAKAO;

        // 회원을 등록합니다.
        Member member = memberService.registerIFNew(socialId, socialType);

        assertThat(member.getId()).isNotNull();
        assertThat(member.getSocialId()).isEqualTo(socialId);

        Member resultMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(resultMember.getSocialId()).isEqualTo(socialId);
    }

    @Test
    @DisplayName("이미 존재하는 소셜 ID로 가입 시 기존 회원을 반환해야 한다.")
    public void should_ReturnExist_when_Register() {
        // 있는 회원을 반환합니다.
        Member existingMember =
                memberService.registerIFNew(memberA.getSocialId(), memberA.getSocialType());

        assertThat(memberA.getId()).isEqualTo(existingMember.getId());
        assertThat(memberA.getSocialId()).isEqualTo(existingMember.getSocialId());
    }

    @Test
    @DisplayName("memberId로 조회한 멤버를 삭제한다.")
    public void should_deleteMember() {
        memberService.deleteMember(memberA.getId());

        Optional<Member> deletedMember = memberRepository.findById(memberA.getId());
        assertThat(deletedMember).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 회원을 삭제하려고 하면 예외가 발생한다.")
    public void should_TrowException_when_DeleteMember() {
        UUID memberId = UUID.randomUUID();

        assertThatThrownBy(
                        () -> {
                            memberService.deleteMember(memberId);
                        })
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }
}
