package com.advent.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.advent.backend.entity.Member;
import com.advent.backend.entity.Store;
import com.advent.backend.entity.Subscription;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubscriptionRepositoryTest {

    @Autowired private SubscriptionRepository subscriptionRepository;

    @Autowired private MemberRepository memberRepository;

    @Autowired private StoreRepository storeRepository;

    private Member subscriber;
    private Member owner;
    private Store store;

    @BeforeEach
    void setUp() {
        subscriber =
                Member.builder()
                        .nickname("구독왕")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("subscriber_1")
                        .point(0)
                        .build();
        memberRepository.save(subscriber);

        owner =
                Member.builder()
                        .nickname("사장님")
                        .socialType(Member.SocialType.KAKAO)
                        .socialId("owner_1")
                        .point(0)
                        .build();
        memberRepository.save(owner);

        store = Store.builder().member(owner).title("맛집").build();
        storeRepository.save(store);
    }

    @Test
    @DisplayName("멤버 ID로 구독 목록을 페이징하여 조회할 수 있다.")
    void should_ReturnPagedSubscriptions_When_FindAllByMemberId() {
        for (int i = 0; i < 5; i++) {
            Member extraOwner =
                    Member.builder()
                            .nickname("사장님_" + i)
                            .socialType(Member.SocialType.KAKAO)
                            .socialId("owner_" + i)
                            .point(0)
                            .build();
            memberRepository.save(extraOwner);

            Store extraStore = Store.builder().member(extraOwner).title("가게 " + i).build();
            storeRepository.save(extraStore);

            Subscription sub =
                    Subscription.builder()
                            .member(subscriber)
                            .store(extraStore)
                            .isNotificated(true)
                            .build();
            subscriptionRepository.save(sub);
        }

        Pageable pageable = PageRequest.of(0, 3, Sort.by("createdAt").descending());

        Page<Subscription> result =
                subscriptionRepository.findAllByMemberId(subscriber.getId(), pageable);

        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("멤버 ID와 상점 ID로 구독 여부를 확인할 수 있다 (존재할 때).")
    void should_ReturnTrue_When_SubscriptionExists() {
        Subscription subscription =
                Subscription.builder().member(subscriber).store(store).isNotificated(true).build();
        subscriptionRepository.save(subscription);

        boolean exists =
                subscriptionRepository.existsByMemberIdAndStoreId(
                        subscriber.getId(), store.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("멤버 ID와 상점 ID로 구독 여부를 확인할 수 있다 (존재하지 않을 때).")
    void should_ReturnFalse_When_SubscriptionDoesNotExist() {
        boolean exists =
                subscriptionRepository.existsByMemberIdAndStoreId(
                        subscriber.getId(), store.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("멤버 ID와 상점 ID로 특정 구독 정보를 조회할 수 있다.")
    void should_ReturnSubscription_When_FindByMemberIdAndStoreId() {
        Subscription subscription =
                Subscription.builder().member(subscriber).store(store).isNotificated(true).build();
        subscriptionRepository.save(subscription);

        Optional<Subscription> foundSub =
                subscriptionRepository.findByMemberIdAndStoreId(subscriber.getId(), store.getId());

        assertThat(foundSub).isPresent();
        assertThat(foundSub.get().getMember().getId()).isEqualTo(subscriber.getId());
        assertThat(foundSub.get().getStore().getId()).isEqualTo(store.getId());
    }

    @Test
    @DisplayName("구독 정보를 삭제할 수 있다.")
    void should_DeleteSubscription_When_EntityDeleted() {
        Subscription subscription = Subscription.builder().member(subscriber).store(store).build();
        subscriptionRepository.save(subscription);

        subscriptionRepository.delete(subscription);

        boolean exists =
                subscriptionRepository.existsByMemberIdAndStoreId(
                        subscriber.getId(), store.getId());
        assertThat(exists).isFalse();
    }
}
