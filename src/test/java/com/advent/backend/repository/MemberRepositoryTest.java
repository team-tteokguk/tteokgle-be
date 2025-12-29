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

import java.util.Optional;

@DataJpaTest
@Rollback(false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MemberRepositoryTest {
    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("소셜 ID와 소셜 타입으로 유저를 발견")
    void findBySocialIdAndSocialTypeTest() {
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
}
