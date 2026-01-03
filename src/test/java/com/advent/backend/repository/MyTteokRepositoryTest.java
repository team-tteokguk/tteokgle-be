package com.advent.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.advent.backend.entity.Member;
import com.advent.backend.entity.MyTteok;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MyTteokRepositoryTest {

    @Autowired private MyTteokRepository myTteokRepository;

    @Autowired private TestEntityManager em;

    @Test
    @DisplayName("Member ID로 나의 떡국 조회")
    void should_ReturnMyTteok_When_MemberHasTteok() {
        Member member =
                Member.builder()
                        .nickname("마루88")
                        .socialId("maru_88")
                        .socialType(Member.SocialType.KAKAO)
                        .build();
        em.persist(member);

        MyTteok myTteok = MyTteok.create(member);
        em.persist(myTteok);

        em.flush();
        em.clear();

        Optional<MyTteok> result = myTteokRepository.findByMemberId(member.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getMember().getNickname()).isEqualTo("마루88");
    }

    @Test
    @DisplayName("아직 떡국 만들지 않은 사용자")
    void should_ReturnEmpty_When_MemberHasNoTteok() {
        Member newMember =
                Member.builder()
                        .nickname("새로운회원")
                        .socialId("new_user")
                        .socialType(Member.SocialType.KAKAO)
                        .build();
        em.persist(newMember);

        em.flush();
        em.clear();

        Optional<MyTteok> result = myTteokRepository.findByMemberId(newMember.getId());

        assertThat(result).isEmpty();
    }
}
