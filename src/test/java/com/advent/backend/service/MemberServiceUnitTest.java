package com.advent.backend.service;

import static org.mockito.BDDMockito.given;

import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.repository.MemberRepository;
import com.advent.backend.repository.MyTteokRepository;
import com.advent.backend.repository.StoreRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MemberServiceUnitTest {
    @InjectMocks private MemberService memberService;

    @Mock MemberRepository memberRepository;
    @Mock MyTteokRepository myTteokRepository;
    @Mock StoreRepository storeRepository;

    @Test
    @DisplayName("닉네임 유효성 검사를 통과합니다.")
    public void should_SuccessValidTest_when_ValidNickName() {
        String validName = "외요";
        given(memberRepository.existsByNickname(validName)).willReturn(false);

        Assertions.assertDoesNotThrow(
                () -> {
                    memberService.validateNickname(validName);
                });
    }

    @Test
    @DisplayName("유효하지 않은 닉네임으로 닉네임 유효성 검사 시 예외가 발생한다.")
    public void should_FailedValidTest_when_InValidNickName() {
        // 1. 너무 짧은 이름 (1자)
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> {
                            memberService.validateNickname("욍");
                        })
                .isInstanceOf(BusinessException.class);

        // 2. 너무 긴 이름 (12자 초과)
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> {
                            memberService.validateNickname("외요외요외요외요외요외요외요외요외요");
                        })
                .isInstanceOf(BusinessException.class);

        // 3. 금지어 포함
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> {
                            memberService.validateNickname("관리자");
                        })
                .isInstanceOf(BusinessException.class);

        // 4. 중복된 이름
        String duplicateName = "이미 있는 이름";
        given(memberRepository.existsByNickname(duplicateName)).willReturn(true);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> {
                            memberService.validateNickname(duplicateName);
                        })
                .isInstanceOf(BusinessException.class);
    }
}
