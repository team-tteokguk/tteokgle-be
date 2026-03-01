package com.advent.backend.service;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.MyTteok;
import com.advent.backend.entity.Store;
import com.advent.backend.repository.*;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private static final int SIGNUP_BONUS_POINTS = 500;
    private static final String SIGNUP_BONUS_MESSAGE = "회원가입을 환영합니다! 가입 축하금 500엽전을 지급해드렸어요.";
    private static final String SIGNUP_BONUS_LINK = "/my";

    private final MemberRepository memberRepository;
    private final MyTteokRepository myTteokRepository;
    private final StoreRepository storeRepository;
    private final MyItemRepository myItemRepository;
    private final ItemRepository itemRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final GuestBookRepository guestBookRepository;
    private final NotificationRepository notificationRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final NotificationService notificationService;

    private static final String DEFAULT_STORE_TITLE = "나의 상점";

    /** 닉네임 중복 체크 */
    public void validateNickname(String nickname) {
        validateNicknameFormat(nickname);

        // 3. 중복 체크
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    public boolean isNicknameDuplicated(String nickname) {
        validateNicknameFormat(nickname);
        return memberRepository.existsByNickname(nickname);
    }

    private void validateNicknameFormat(String nickname) {
        // 1. 길이 체크 (2 ~ 12자)
        if (nickname == null || nickname.length() > 12 || nickname.length() < 2) {
            throw new BusinessException(ErrorCode.INVALID_NICKNAME_LENGTH);
        }

        // 2. 금지어 체크
        if (containsRestrictedWord(nickname)) {
            throw new BusinessException(ErrorCode.RESTRICTED_NICKNAME);
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

        String previousNickname = member.getNickname();
        member.updateNickname(nickname);

        // 최초 닉네임 설정 시점에는 상점명을 "<닉네임>의 상점"으로 초기화한다.
        if (previousNickname == null || previousNickname.isBlank()) {
            storeRepository
                    .findByMemberId(MemberId)
                    .ifPresent(
                            store -> {
                                if (DEFAULT_STORE_TITLE.equals(store.getTitle())) {
                                    store.updateTitle(buildInitialStoreTitle(nickname));
                                }
                            });
        }
    }

    @Transactional
    public void updateProfileImage(UUID memberId, String profileImage) {
        validateProfileImage(profileImage);

        Member member =
                memberRepository
                        .findById(memberId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.updateProfileImage(profileImage.trim());
    }

    /**
     * 최초 회원 등록, social 아이디로 멤버 여부 확인함
     *
     * @param socialId
     * @param socialType
     * @return social 아이디가 있으면 기존 멤버 반환, 없으면 새 멤버 생성 후 반환
     */
    @Transactional
    public Member registerIFNew(String socialId, Member.SocialType socialType) {
        Member member = memberRepository.findBySocialId(socialId).orElse(null);
        boolean isNewMember = member == null;

        if (isNewMember) {
            member =
                    memberRepository.save(
                            Member.builder()
                                    .socialId(socialId)
                                    .socialType(socialType)
                                    .point(SIGNUP_BONUS_POINTS)
                                    .build());
            notificationService.sendSystemNotification(
                    member, SIGNUP_BONUS_MESSAGE, SIGNUP_BONUS_LINK);
        }

        createDefaultAssetsIfAbsent(member);
        return member;
    }

    private void createDefaultAssetsIfAbsent(Member member) {
        if (!myTteokRepository.existsByMemberId(member.getId())) {
            myTteokRepository.save(MyTteok.create(member));
        }

        if (!storeRepository.existsByMemberId(member.getId())) {
            storeRepository.save(
                    Store.create(member, buildInitialStoreTitle(member.getNickname())));
        }
    }

    private String buildInitialStoreTitle(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return DEFAULT_STORE_TITLE;
        }
        return nickname + "의 상점";
    }

    private void validateProfileImage(String profileImage) {
        if (profileImage == null || profileImage.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String trimmed = profileImage.trim();
        if (trimmed.length() > 2048) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Transactional
    public void deleteMember(UUID MemberId) {
        Member member =
                memberRepository
                        .findById(MemberId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 1) 내가 소유한 상점 연관 데이터 정리
        storeRepository
                .findByMemberId(MemberId)
                .ifPresent(
                        store -> {
                            UUID storeId = store.getId();

                            // 다른 사용자의 구독/방명록/공유링크 제거
                            subscriptionRepository.deleteAllByStoreId(storeId);
                            guestBookRepository.deleteAllByStoreId(storeId);
                            shareLinkRepository.deleteAllByStoreId(storeId);

                            // 판매 아이템을 참조하는 구매자 my_item 제거 후 아이템 삭제
                            myItemRepository.deleteAllByItem_Store_Id(storeId);
                            itemRepository.deleteAllByStoreId(storeId);
                        });
        storeRepository.deleteAllByMemberId(MemberId);
        memberRepository.flush();

        // 2) 회원이 작성/소유한 데이터 정리
        subscriptionRepository.deleteAllByMemberId(MemberId);
        guestBookRepository.deleteAllByMemberId(MemberId);
        notificationRepository.deleteAllByMemberId(MemberId);
        memberRepository.flush();
        myItemRepository.deleteAllByMemberId(MemberId);

        // 3) 떡국 및 떡국 연관 my_item 정리
        myTteokRepository
                .findByMemberId(MemberId)
                .ifPresent(myTteok -> myItemRepository.deleteAllByTteokId(myTteok.getId()));
        myTteokRepository.deleteAllByMemberId(MemberId);
        memberRepository.flush();

        // 4) 포인트 기록/리프레시 토큰 정리
        pointHistoryRepository.deleteAllBySenderIdOrReceiverId(MemberId);
        refreshTokenRepository.deleteByAuthKey(MemberId.toString());

        // 5) 회원 삭제
        memberRepository.delete(member);
    }
}
