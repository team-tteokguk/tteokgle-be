package com.advent.backend.repository;

import com.advent.backend.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MemberRepositoryTest {
    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 소셜_아이디와_소셜_타입으로_유저를_조회_테스트() {
        String socialId = "unique_social_id_123";
        Member.SocialType socialType = Member.SocialType.KAKAO;

        Member member = Member.builder()
                .socialId(socialId)
                .socialType(socialType)
                .nickname("외요3")
                .build();

        memberRepository.save(member);
        Optional<Member> result = memberRepository.findBySocialTypeAndSocialId(socialType, socialId);

        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(member);
        assertThat(result.get().getSocialType()).isEqualTo(socialType);
        assertThat(result.get().getSocialId()).isEqualTo(socialId);
        assertThat(result.get().getNickname()).isEqualTo("외요3");
    }

    @Test
    void 포인트_두_멤버_차감_증가_테스트() {
        Member buyer = Member.builder()
                .socialId("buyer").nickname("구매자").point(1000).build();
        Member seller = Member.builder()
                .socialId("seller").nickname("판매자").point(0).build();

        memberRepository.save(buyer);
        memberRepository.save(seller);

        Member lockedBuyer = memberRepository.findByIdWithLock(buyer.getId()).orElseThrow();
        Member lockedSeller = memberRepository.findByIdWithLock(seller.getId()).orElseThrow();

        int price = 300;
        lockedBuyer.decreasePoint(price); // 1000 -> 700
        lockedSeller.addPoint(price);     // 0 -> 300

        // 3. Then: 데이터 검증
        assertThat(lockedBuyer.getPoint()).isEqualTo(700);
        assertThat(lockedSeller.getPoint()).isEqualTo(300);
    }

    @Test
    void 포인트_특정_멤버_일괄_지급_테스트() {
        String socialId = "unique_social_id_123";
        Member.SocialType socialType = Member.SocialType.KAKAO;

        Member member = Member.builder()
                .socialId(socialId)
                .socialType(socialType)
                .nickname("외요")
                .build();

        Member member2 = Member.builder()
                .socialId(socialId)
                .socialType(socialType)
                .nickname("마루")
                .build();

        memberRepository.save(member);
        memberRepository.save(member2);

        List<UUID> ids = List.of(member.getId(),member2.getId());

        int updatedCount = memberRepository.addPointToMembers(500, ids);
        assertThat(updatedCount).isEqualTo(2);

    }

    @Test
    void 포인트_모든_멤버_일괄_지급_테스트() {
        String socialId = "unique_social_id_123";
        Member.SocialType socialType = Member.SocialType.KAKAO;

        Member member = Member.builder()
                .socialId(socialId)
                .socialType(socialType)
                .nickname("외요")
                .build();

        Member member2 = Member.builder()
                .socialId(socialId)
                .socialType(socialType)
                .nickname("마루")
                .build();

        memberRepository.save(member);
        memberRepository.save(member2);

        int updatedCount = memberRepository.addPointToAllMemebers(500);
        assertThat(updatedCount).isEqualTo(2);
    }
}
