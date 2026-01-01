package com.advent.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.advent.backend.entity.Member;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MemberRepositoryTest {
    @Autowired private MemberRepository memberRepository;

    @Test
    @DisplayName("소셜아이디와 소셜타입으로 멤버 조회")
    void should_FindMember_When_SocialIdAndTypeMatch() {
        String socialId = "unique_social_id_123";
        Member.SocialType socialType = Member.SocialType.KAKAO;

        Member member =
                Member.builder().socialId(socialId).socialType(socialType).nickname("외요3").build();

        memberRepository.save(member);
        Optional<Member> result =
                memberRepository.findBySocialTypeAndSocialId(socialType, socialId);

        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(member);
        assertThat(result.get().getSocialType()).isEqualTo(socialType);
        assertThat(result.get().getSocialId()).isEqualTo(socialId);
        assertThat(result.get().getNickname()).isEqualTo("외요3");
    }

    @Test
    @DisplayName("두 멤버 포인트 증감 테스트")
    void should_UpdatePoints_when_PointsTransferredBetweenMembers() {
        Member.SocialType socialType = Member.SocialType.KAKAO;

        Member buyer =
                Member.builder()
                        .socialId("buyer")
                        .socialType(socialType)
                        .nickname("구매자")
                        .point(1000)
                        .build();

        Member seller =
                Member.builder()
                        .socialId("seller")
                        .socialType(socialType)
                        .nickname("판매자")
                        .point(0)
                        .build();

        memberRepository.save(buyer);
        memberRepository.save(seller);

        Member lockedBuyer = memberRepository.findByIdWithLock(buyer.getId()).orElseThrow();
        Member lockedSeller = memberRepository.findByIdWithLock(seller.getId()).orElseThrow();

        int price = 300;
        lockedBuyer.decreasePoint(price); // 1000 -> 700
        lockedSeller.addPoint(price); // 0 -> 300

        // 3. Then: 데이터 검증
        assertThat(lockedBuyer.getPoint()).isEqualTo(700);
        assertThat(lockedSeller.getPoint()).isEqualTo(300);
    }

    @Test
    @DisplayName("특정 멤버 포인트 일괄 지급 테스트")
    void should_UpdatePointsForTargets_when_BatchDistributed() {
        String socialId = "unique_social_id_123";
        Member.SocialType socialType = Member.SocialType.KAKAO;

        Member member =
                Member.builder().socialId(socialId).socialType(socialType).nickname("외요").build();

        Member member2 =
                Member.builder().socialId(socialId).socialType(socialType).nickname("마루").build();

        memberRepository.save(member);
        memberRepository.save(member2);

        List<UUID> ids = List.of(member.getId(), member2.getId());

        int updatedCount = memberRepository.addPointToMembers(500, ids);
        assertThat(updatedCount).isEqualTo(2);
    }

    @Test
    @DisplayName("모든 멤버 포인트 일괄 지급 테스트")
    void should_UpdatePointsForAll_when_BatchDistributed() {
        String socialId = "unique_social_id_123";
        Member.SocialType socialType = Member.SocialType.KAKAO;

        Member member =
                Member.builder().socialId(socialId).socialType(socialType).nickname("외요").build();

        Member member2 =
                Member.builder().socialId(socialId).socialType(socialType).nickname("마루").build();

        memberRepository.save(member);
        memberRepository.save(member2);

        int updatedCount = memberRepository.addPointToAllMemebers(500);
        assertThat(updatedCount).isEqualTo(2);
    }
}
