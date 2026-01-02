package com.advent.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.advent.backend.entity.Member;
import com.advent.backend.entity.Store;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class StoreRepositoryTest {

    @Autowired private StoreRepository storeRepository;

    @Autowired private TestEntityManager em;

    private Member savedMember;
    private Store savedStore;

    @BeforeEach
    void setUp() {
        Member member =
                Member.builder()
                        .socialType(Member.SocialType.GOOGLE)
                        .socialId("unique_social_id_456")
                        .nickname("마루")
                        .point(0)
                        .build();

        savedMember = em.persist(member);

        Store store = Store.builder().member(savedMember).title("마루네 고명집").build();

        savedStore = em.persist(store);

        em.flush();
        em.clear();
    }

    // existsByIdAndMemberId 테스트

    @Test
    @DisplayName("유효한 상점 ID와 회원 ID 테스트")
    void should_ReturnTrue_When_ValidStoreAndMemberId() {
        // Given
        UUID storeId = savedStore.getId();
        UUID memberId = savedMember.getId();

        boolean result = storeRepository.existsByIdAndMemberId(storeId, memberId);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 상점 ID 테스트")
    void should_ReturnFalse_When_StoreIdNotExists() {
        UUID randomStoreId = UUID.randomUUID();
        UUID memberId = savedMember.getId();

        boolean result = storeRepository.existsByIdAndMemberId(randomStoreId, memberId);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("회원 ID 불일치 테스트")
    void should_ReturnFalse_When_MemberIdNotMatches() {
        UUID storeId = savedStore.getId();
        UUID otherMemberId = UUID.randomUUID();

        boolean result = storeRepository.existsByIdAndMemberId(storeId, otherMemberId);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("상점 ID가 Null 일 때 테스트")
    void should_ReturnFalse_When_StoreIdIsNull() {
        boolean result = storeRepository.existsByIdAndMemberId(null, savedMember.getId());
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("회원 ID Null 일 때 테스트")
    void should_ReturnFlase_When_MemberIdIsNull() {
        boolean result = storeRepository.existsByIdAndMemberId(savedStore.getId(), null);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("상점 ID와 회원 ID 모두 Null일 때 테스트")
    void should_RetrunFalse_When_BothIdsAreNull() {
        boolean result = storeRepository.existsByIdAndMemberId(null, null);
        assertThat(result).isFalse();
    }

    // findByMemberId 테스트

    @Test
    @DisplayName("존재하는 회원 ID로 조회")
    void should_ReturnStore_When_MemberIdExists() {
        Optional<Store> result = storeRepository.findByMemberId(savedMember.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedStore.getId());
        assertThat(result.get().getTitle()).isEqualTo("마루네 고명집");
        assertThat(result.get().getMember().getId()).isEqualTo(savedMember.getId());
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID 조회")
    void should_ReturnEmptyOptional_When_MemberIdNotExists() {
        Optional<Store> result = storeRepository.findByMemberId(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("회원 ID가 Null일 일 때 테스트")
    void should_ReturnEmptyOptional_When_MemberIdsNull() {
        Optional<Store> result = storeRepository.findByMemberId(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상점을 보유하지 않은 회원 ID 조회")
    void should_ReturnEmptyOptional_When_MemberHasNoStore() {
        Member newMember =
                Member.builder()
                        .socialId("unique_social_id_789")
                        .socialType(Member.SocialType.KAKAO)
                        .nickname("김외요")
                        .build();
        em.persist(newMember);
        em.flush();

        Optional<Store> result = storeRepository.findByMemberId(newMember.getId());

        assertThat(result).isEmpty();
    }

    // findByMember 테스트

    @Test
    @DisplayName("유효한 회원 객체 조회")
    void should_ReturnStore_When_ValidMemberObject() {
        Member foundMember = em.find(Member.class, savedMember.getId());
        Optional<Store> result = storeRepository.findByMember(foundMember);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedStore.getId());
    }

    @Test
    @DisplayName("상점을 보유하지 않은 회원 객체 조회")
    void should_ReturnEmptyOptional_When_MemberObjectHasNoStore() {
        Member noStoreMember =
                Member.builder()
                        .socialId("unique_social_id_111")
                        .socialType(Member.SocialType.GOOGLE)
                        .nickname("마루2")
                        .build();
        em.persist(noStoreMember);
        em.flush();

        Optional<Store> result = storeRepository.findByMember(noStoreMember);

        assertThat(result).isEmpty();
    }

    // findByTitle 테스트

    @Test
    @DisplayName("존재하는 상점명 조회")
    void should_ReturnStore_When_TitleExists() {
        Optional<Store> result = storeRepository.findByTitle("마루네 고명집");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedStore.getId());
    }

    @Test
    @DisplayName("존재하지 않는 상점명으로 조회")
    void should_ReturnEmptyOptional_When_TitleNotExists() {
        Optional<Store> result = storeRepository.findByTitle("존재하지 않는 가게");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상점명이 Null일 때")
    void should_ReturnEmptyOptional_When_TitleIsNull() {
        Optional<Store> result = storeRepository.findByTitle(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상점명이 빈 문자열일 때")
    void should_ReturnEmptyOptional_When_TitleIsEmpty() {
        Optional<Store> result = storeRepository.findByTitle("");
        assertThat(result).isEmpty();
    }

    // findByTitleContaining 테스트

    @Test
    @DisplayName("포함된 키워드로 검색")
    void should_ReturnMatchingStores_When_KeywordExists() {

        Member member2 =
                Member.builder()
                        .nickname("주인2")
                        .socialId("social_user_2")
                        .socialType(Member.SocialType.GOOGLE)
                        .build();
        em.persist(member2);

        Store store2 = Store.builder().member(member2).title("마루 고명집2").build();
        em.persist(store2);

        Member member3 =
                Member.builder()
                        .nickname("주인3")
                        .socialId("social_user_3")
                        .socialType(Member.SocialType.GOOGLE)
                        .build();
        em.persist(member3);

        Store store3 = Store.builder().member(member3).title("마루 고명집3").build();
        em.persist(store3);

        em.flush();

        List<Store> results = storeRepository.findByTitleContaining("마루");

        assertThat(results).hasSize(3);
        assertThat(results)
                .extracting("title")
                .containsExactlyInAnyOrder("마루네 고명집", "마루 고명집2", "마루 고명집3");
    }

    @Test
    @DisplayName("일치하는 키워드 없을 때")
    void should_ReturnEmptyList_When_KeywordNotExists() {
        List<Store> results = storeRepository.findByTitleContaining("없어요");
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("검색 키워드가 Null 일 때")
    void should_ReturnEmptyList_When_KeywordIsNull() {
        List<Store> results = storeRepository.findByTitleContaining(null);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("검색 키워드 빈 문자열 일 때")
    void should_ReturnAllStores_When_KeywordIsEmpty() {
        List<Store> results = storeRepository.findByTitleContaining("");
        assertThat(results).isNotEmpty();
    }

    @Test
    @DisplayName("여러 상점 매칭")
    void should_ReturnMultipleStores_When_MultipleMatches() {
        Member member1 =
                Member.builder()
                        .nickname("외요1")
                        .socialId("oeyo1")
                        .socialType(Member.SocialType.KAKAO)
                        .build();
        em.persist(member1);
        Store store2 = Store.builder().member(member1).title("외요네 가게").build();
        em.persist(store2);

        Member member2 =
                Member.builder()
                        .nickname("외요2")
                        .socialId("oeyo2")
                        .socialType(Member.SocialType.KAKAO)
                        .build();
        em.persist(member2);
        Store store3 = Store.builder().member(member2).title("외요네 상점").build();
        em.persist(store3);

        em.flush();

        List<Store> results = storeRepository.findByTitleContaining("외요");

        assertThat(results).hasSize(2);
    }

    // existsByMember 테스트

    @Test
    @DisplayName("상점을 보유한 회원")
    void should_ReturnTrue_When_MemberHasStore() {
        Member foundMember = em.find(Member.class, savedMember.getId());
        boolean result = storeRepository.existsByMember(foundMember);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("상점 미보유 회원")
    void should_ReturnFalse_When_MemberHasNoStore() {
        Member newMember =
                Member.builder()
                        .socialId("unique_social_id_777")
                        .socialType(Member.SocialType.GOOGLE)
                        .nickname("예지")
                        .build();
        em.persist(newMember);
        em.flush();

        boolean result = storeRepository.existsByMember(newMember);

        assertThat(result).isFalse();
    }

    // existsByMemberId 테스트

    @Test
    @DisplayName("상점을 보유한 회원 ID")
    void should_ReturnTrue_When_MemberIdHasStore() {
        boolean result = storeRepository.existsByMemberId(savedMember.getId());
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("상점을 보유하지 않은 회원")
    void should_ReturnFalse_When_MemberIdHasNoStore() {
        Member newMember =
                Member.builder()
                        .socialId("social_666")
                        .socialType(Member.SocialType.KAKAO)
                        .nickname("외요마루")
                        .build();
        em.persist(newMember);
        em.flush();

        boolean result = storeRepository.existsByMemberId(newMember.getId());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID")
    void should_ReturnFalse_When_MemberIdNotExists() {
        boolean result = storeRepository.existsByMemberId(UUID.randomUUID());
        assertThat(result).isFalse();
    }

    // findByIdWithMember 테스트

    @Test
    @DisplayName("상점 ID 조회")
    void should_ReturnStoreWithMember_When_StoreIdExists() {
        em.clear();
        Optional<Store> result = storeRepository.findByIdWithMember(savedStore.getId());

        assertThat(result).isPresent();
        Store store = result.get();
        assertThat(store.getId()).isEqualTo(savedStore.getId());

        Member member = store.getMember();
        assertThat(member).isNotNull();
        assertThat(member.getId()).isEqualTo(savedMember.getId());
        assertThat(member.getNickname()).isEqualTo("마루");
    }

    @Test
    @DisplayName("존재하지 않는 상점 ID 조회")
    void should_ReturnEmptyOptional_When_StoreIdNotExistsWithJoinFetch() {
        Optional<Store> result = storeRepository.findByIdWithMember(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    // findTop10ByOderByCreatedAtDesc 테스트

    @Test
    @DisplayName("TOP 10 상점 조회 초과 상황")
    void should_Return10Stores_When_15StoresExist() {
        for (int i = 1; i <= 15; i++) {
            Member newMember =
                    Member.builder()
                            .nickname("주인" + i)
                            .socialId("bulk_social_" + i)
                            .socialType(Member.SocialType.GOOGLE)
                            .build();
            em.persist(newMember);

            Store store = Store.builder().member(newMember).title("가게" + i).build();
            em.persist(store);
        }
        em.flush();

        List<Store> results = storeRepository.findTop10ByOrderByCreatedAtDesc();

        assertThat(results).hasSize(10);
    }

    @Test
    @DisplayName("TOP10 상점 조회 미달 상황")
    void should_Return5Stores_When_Only5StoresExist() {
        for (int i = 1; i <= 4; i++) {
            Member newMember =
                    Member.builder()
                            .nickname("소규모주인" + i)
                            .socialId("small_social_" + i)
                            .socialType(Member.SocialType.GOOGLE)
                            .build();
            em.persist(newMember);

            Store store = Store.builder().member(newMember).title("가게" + i).build();
            em.persist(store);
        }
        em.flush();

        List<Store> results = storeRepository.findTop10ByOrderByCreatedAtDesc();

        assertThat(results).hasSize(5);
    }

    @Test
    @DisplayName("상점 목록 최신순 정렬 확인")
    void should_ReturnStoresInDescOrder_When_MultipleStoresExist() {
        storeRepository.deleteAll();
        em.flush();

        Member member1 =
                Member.builder()
                        .nickname("테스터1")
                        .socialId("tester1")
                        .socialType(Member.SocialType.GOOGLE)
                        .build();
        em.persist(member1);

        Store oldStore = Store.builder().member(member1).title("오래된 가게").build();
        em.persist(oldStore);
        em.flush();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Member member2 =
                Member.builder()
                        .nickname("테스터2")
                        .socialId("tester2")
                        .socialType(Member.SocialType.GOOGLE)
                        .build();
        em.persist(member2);

        Store newStore = Store.builder().member(member2).title("새로운 가게").build();
        em.persist(newStore);
        em.flush();

        List<Store> results = storeRepository.findTop10ByOrderByCreatedAtDesc();

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getCreatedAt())
                .isAfterOrEqualTo(results.get(results.size() - 1).getCreatedAt());
    }

    @Test
    @DisplayName("저장된 상점이 없을 때")
    void should_ReturnEmptyList_When_NoStoresExist() {
        storeRepository.deleteAll();
        em.flush();

        List<Store> results = storeRepository.findTop10ByOrderByCreatedAtDesc();

        assertThat(results).isEmpty();
    }

    // 통합 테스트

    // findByMemberNickname 테스트

    @Test
    @DisplayName("회원 닉네임으로 상점 조회 존재하는 경우")
    void should_ReturnStore_When_MemberNicknameExists() {
        Optional<Store> result = storeRepository.findByMemberNickname("마루");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedStore.getId());
        assertThat(result.get().getTitle()).isEqualTo("마루네 고명집");
        assertThat(result.get().getMember().getNickname()).isEqualTo("마루");
    }

    @Test
    @DisplayName("회원 닉네임으로 상점 조회 존재하지 않는 경우")
    void should_ReturnEmptyOptional_When_MemberNicknameNotExists() {
        Optional<Store> result = storeRepository.findByMemberNickname("존재하지않는닉네임");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("회원 닉네임이 Null 일 때")
    void should_ReturnEmptyOptional_When_MemberNicknameIsNull() {
        Optional<Store> result = storeRepository.findByMemberNickname(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상점이 없는 회원 닉네임으로 조회")
    void should_ReturnEmptyOptional_When_MemberHasNoStoreByNickname() {
        Member noStoreMember =
                Member.builder()
                        .socialId("no_store_user")
                        .socialType(Member.SocialType.KAKAO)
                        .nickname("상점없음")
                        .build();
        em.persist(noStoreMember);
        em.flush();

        Optional<Store> result = storeRepository.findByMemberNickname("상점없음");
        assertThat(result).isEmpty();
    }

    // findByMemberNicknameContaining 테스트

    @Test
    @DisplayName("회원 닉네임 포함 검색 - 단일 결과")
    void should_ReturnMatchingStores_When_NicknameContainsKeyword() {
        List<Store> results = storeRepository.findByMemberNicknameContaining("마루");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMember().getNickname()).isEqualTo("마루");
        assertThat(results.get(0).getTitle()).isEqualTo("마루네 고명집");
    }

    @Test
    @DisplayName("회원 닉네임 포함 검색 - 복수 결과")
    void should_ReturnMultipleStores_When_MultipleNicknamesMatch() {
        Member member1 =
                Member.builder()
                        .nickname("김철수")
                        .socialId("kim1")
                        .socialType(Member.SocialType.GOOGLE)
                        .build();
        em.persist(member1);

        Store store1 = Store.builder().member(member1).title("김철수네 가게").build();
        em.persist(store1);

        Member member2 =
                Member.builder()
                        .nickname("김영희")
                        .socialId("kim2")
                        .socialType(Member.SocialType.KAKAO)
                        .build();
        em.persist(member2);

        Store store2 = Store.builder().member(member2).title("김영희네 상점").build();
        em.persist(store2);

        em.flush();

        List<Store> results = storeRepository.findByMemberNicknameContaining("김");

        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(store -> store.getMember().getNickname())
                .containsExactlyInAnyOrder("김철수", "김영희");
    }

    @Test
    @DisplayName("회원 닉네임 포함 검색 - 일치하는 결과 없음")
    void should_ReturnEmptyList_When_NoNicknameMatches() {
        List<Store> results = storeRepository.findByMemberNicknameContaining("없는닉네임");
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("회원 닉네임 검색 키워드가 Null일 때")
    void should_ReturnEmptyList_When_NicknameKeywordIsNull() {
        List<Store> results = storeRepository.findByMemberNicknameContaining(null);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("회원 닉네임 검색 키워드가 빈 문자열일 때")
    void should_ReturnAllStores_When_NicknameKeywordIsEmpty() {
        List<Store> results = storeRepository.findByMemberNicknameContaining("");
        assertThat(results).isNotEmpty();
    }

    @Test
    @DisplayName("상점이 없는 회원은 검색 결과에 포함되지 않음")
    void should_NotReturnMembersWithoutStores_When_SearchingByNickname() {
        Member noStoreMember =
                Member.builder()
                        .socialId("no_store_test")
                        .socialType(Member.SocialType.GOOGLE)
                        .nickname("마루마루")
                        .build();
        em.persist(noStoreMember);
        em.flush();

        List<Store> results = storeRepository.findByMemberNicknameContaining("마루");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMember().getNickname()).isEqualTo("마루");
    }

    // findByMemberNicknameContaining 테스트

    @Test
    @DisplayName("상점 생성 > 삭제 > 재생성 흐름")
    void should_WorkCorrectly_When_CreateDeleteAndRecreateStore() {

        Member newMember =
                Member.builder()
                        .socialId("userScenario")
                        .socialType(Member.SocialType.GOOGLE)
                        .nickname("왹저")
                        .build();
        em.persist(newMember);
        em.flush();

        UUID memberId = newMember.getId();

        assertThat(storeRepository.existsByMemberId(memberId)).isFalse();

        Store store1 = Store.builder().member(newMember).title("시나리오 가게 1").build();
        storeRepository.save(store1);

        assertThat(storeRepository.existsByMemberId(memberId)).isTrue();
        assertThat(storeRepository.findByMemberId(memberId))
                .isPresent()
                .get()
                .extracting("title")
                .isEqualTo("시나리오 가게 1");

        storeRepository.delete(store1);
        em.flush(); // 삭제 반영

        assertThat(storeRepository.existsByMemberId(memberId)).isFalse();

        Store store2 = Store.builder().member(newMember).title("시나리오 가게 2").build();
        storeRepository.save(store2);

        assertThat(storeRepository.existsByMemberId(memberId)).isTrue();
        Optional<Store> finalStore = storeRepository.findByMemberId(memberId);
        assertThat(finalStore).isPresent();
        assertThat(finalStore.get().getTitle()).isEqualTo("시나리오 가게 2");
    }

    @Test
    @DisplayName("데이터 무결성")
    void should_MaintainDataIntegrity_When_MultipleConcurrentOperations() {

        Member member1 =
                Member.builder()
                        .socialId("concurrent1")
                        .socialType(Member.SocialType.KAKAO)
                        .nickname("동시1")
                        .build();
        em.persist(member1);

        Member member2 =
                Member.builder()
                        .socialId("concurrent2")
                        .socialType(Member.SocialType.KAKAO)
                        .nickname("동시2")
                        .build();
        em.persist(member2);

        Store store1 = Store.builder().member(member1).title("동시 가게 1").build();
        em.persist(store1);

        Store store2 = Store.builder().member(member2).title("동시 가게 2").build();
        em.persist(store2);
        em.flush();

        UUID store1Id = store1.getId();
        UUID member1Id = member1.getId();
        UUID store2Id = store2.getId();
        UUID member2Id = member2.getId();

        assertThat(storeRepository.existsByIdAndMemberId(store1Id, member1Id)).isTrue();
        assertThat(storeRepository.existsByIdAndMemberId(store2Id, member2Id)).isTrue();

        assertThat(storeRepository.existsByIdAndMemberId(store1Id, member2Id)).isFalse();
        assertThat(storeRepository.existsByIdAndMemberId(store2Id, member1Id)).isFalse();
    }
}
