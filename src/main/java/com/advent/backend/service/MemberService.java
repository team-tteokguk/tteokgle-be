package com.advent.backend.service;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.entity.Member;
import com.advent.backend.repository.MemberRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    /** 닉네임 중복 체크 */
    public void validateNickname(String nickname) {
        // 1. 길이 체크 (2 ~ 10자)
        if (nickname == null || nickname.length() > 12 || nickname.length() < 2) {
            throw new BusinessException(ErrorCode.INVALID_NICKNAME_LENGTH);
        }

        // 2. 금지어 체크
        if (containsRestrictedWord(nickname)) {
            throw new BusinessException(ErrorCode.RESTRICTED_NICKNAME);
        }

        // 3. 중복 체크
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private boolean containsRestrictedWord(String nickname) {
        List<String> badWords = List.of("관리자", "운영자", "admin");
        return badWords.stream().anyMatch(nickname::contains);
    }

    /**
     * 닉네임 업데이트 (최초 등록 포함)
     *
     * @param MemberId
     * @param nickname
     */
    @Transactional
    public void updateNickName(UUID MemberId, String nickname) {
        validateNickname(nickname);

        Member member =
                memberRepository
                        .findById(MemberId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        member.updateNickname(nickname);
    }

    /**
     * 최초 회원 등록, social 아이디로 멤버 여부 확인함
     *
     * @param socialId
     * @param socialType
     * @return social 아이디가 있으면 기존 멤버 반환, 없으면 새 멤버 생성 후 반환
     */
    public Member registerIFNew(String socialId, Member.SocialType socialType) {
        return memberRepository
                .findBySocialId(socialId)
                .orElseGet(
                        () ->
                                memberRepository.save(
                                        Member.builder()
                                                .socialId(socialId)
                                                .socialType(socialType)
                                                .build()));
    }

    @Transactional
    public void deleteMember(UUID MemberId) {
        Member member =
                memberRepository
                        .findById(MemberId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        memberRepository.delete(member);
    }
}
