package com.advent.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.advent.backend.entity.GuestBook;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.Store;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GuestBookRepositoryTest {

    @Autowired private GuestBookRepository guestBookRepository;

    @Autowired private TestEntityManager em;

    private Member savedMember;
    private Store savedStore;
    private GuestBook savedGuestBook;

    @BeforeEach
    void setUp() {
        Member member =
                Member.builder()
                        .nickname("마루")
                        .socialId("test_social_id")
                        .socialType(Member.SocialType.GOOGLE)
                        .build();
        savedMember = em.persist(member);

        Store store = Store.builder().member(savedMember).title("마루네 고명집").build();
        savedStore = em.persist(store);

        GuestBook guestBook = GuestBook.write(savedMember, savedStore, "새해 복 많이 받으세요!");
        savedGuestBook = em.persist(guestBook);

        em.flush();
        em.clear();
    }

    // 조회 및 Fetch Join 테스트

    @Test
    @DisplayName("상점 객체로 방명록 조회")
    void should_ReturnGuestBookPage_When_StoreObjectProvided() {
        GuestBook g1 = GuestBook.write(savedMember, savedStore, "추가 글 1");
        GuestBook g2 = GuestBook.write(savedMember, savedStore, "추가 글 2");
        em.persist(g1);
        em.persist(g2);
        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(0, 10);
        Slice<GuestBook> result = guestBookRepository.findAllByStore(savedStore, pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getContent().get(0).getContent()).isNotNull();
    }

    @Test
    @DisplayName("상점 ID로 방명록 조회")
    void should_ReturnGuestBooksWithMember_When_StoreIdExists() {
        Pageable pageable = PageRequest.of(0, 10);

        Slice<GuestBook> result =
                guestBookRepository.findAllByStoreIdWithMember(savedStore.getId(), pageable);

        assertThat(result.getContent()).hasSize(1);

        GuestBook findGuestBook = result.getContent().get(0);

        assertThat(findGuestBook.getContent()).isEqualTo("새해 복 많이 받으세요!");

        assertThat(findGuestBook.getMember()).isNotNull();
        assertThat(findGuestBook.getMember().getNickname()).isEqualTo("마루");
    }

    @Test
    @DisplayName("방명록 상세 조회")
    void should_ReturnGuestBookWithMember_When_GuestBookIdExists() {
        Optional<GuestBook> result = guestBookRepository.findByIdWithMember(savedGuestBook.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("새해 복 많이 받으세요!");
        assertThat(result.get().getMember().getNickname()).isEqualTo("마루");
    }

    // 권한 체크 (exists) 테스트

    @Test
    @DisplayName("내 방명록인 경우")
    void should_ReturnTrue_When_GuestBookIdAndMemberIdMatch() {
        boolean exists =
                guestBookRepository.existsByIdAndMemberId(
                        savedGuestBook.getId(), savedMember.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("남의 방명록인 경우")
    void should_ReturnFalse_When_MemberIdDoesNotMatch() {
        Member otherMember =
                Member.builder()
                        .nickname("도둑")
                        .socialId("thief_id")
                        .socialType(Member.SocialType.KAKAO)
                        .build();
        em.persist(otherMember);
        em.flush();

        boolean exists =
                guestBookRepository.existsByIdAndMemberId(
                        savedGuestBook.getId(), otherMember.getId());

        assertThat(exists).isFalse();
    }
}
