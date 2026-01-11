package com.advent.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.SubscriptionDto;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.Store;
import com.advent.backend.entity.Subscription;
import com.advent.backend.event.SubscribeEvent;
import com.advent.backend.repository.MemberRepository;
import com.advent.backend.repository.StoreRepository;
import com.advent.backend.repository.SubscriptionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @InjectMocks private SubscriptionService subscriptionService;

    @Mock private SubscriptionRepository subscriptionRepository;

    @Mock private MemberRepository memberRepository;

    @Mock private StoreRepository storeRepository;

    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("정상적으로 구독하고 알림 이벤트를 발행해야 한다")
    void should_SaveSubscriptionAndPublishEvent_When_ValidRequest() {
        UUID subscriberId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Member subscriber = Member.builder().id(subscriberId).nickname("구독자").build();
        Member owner = Member.builder().id(ownerId).nickname("사장님").build();
        Store store = Store.builder().id(storeId).member(owner).title("맛집").build();

        given(memberRepository.findById(subscriberId)).willReturn(Optional.of(subscriber));
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(subscriptionRepository.existsByMemberIdAndStoreId(subscriberId, storeId))
                .willReturn(false);

        subscriptionService.subscribe(subscriberId, storeId);

        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
        verify(eventPublisher, times(1)).publishEvent(any(SubscribeEvent.class));
    }

    @Test
    @DisplayName("본인의 상점을 구독하려고 하면 예외가 발생해야 한다")
    void should_ThrowException_When_SelfSubscription() {
        UUID memberId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        Member member = Member.builder().id(memberId).build();
        Store myStore = Store.builder().id(storeId).member(member).build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(storeRepository.findById(storeId)).willReturn(Optional.of(myStore));

        assertThatThrownBy(() -> subscriptionService.subscribe(memberId, storeId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SELF_SUBSCRIPTION_NOT_ALLOWED);

        verify(subscriptionRepository, times(0)).save(any());
        verify(eventPublisher, times(0)).publishEvent(any());
    }

    @Test
    @DisplayName("이미 구독 중인 상점을 다시 구독하려고 하면 예외가 발생해야 한다")
    void should_ThrowException_When_AlreadySubscribed() {
        UUID subscriberId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Member subscriber = Member.builder().id(subscriberId).build();
        Member owner = Member.builder().id(ownerId).build();
        Store store = Store.builder().id(storeId).member(owner).build();

        given(memberRepository.findById(subscriberId)).willReturn(Optional.of(subscriber));
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(subscriptionRepository.existsByMemberIdAndStoreId(subscriberId, storeId))
                .willReturn(true);

        assertThatThrownBy(() -> subscriptionService.subscribe(subscriberId, storeId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALREADY_SUBSCRIBED);

        verify(subscriptionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 유저나 상점으로 구독 시도 시 예외가 발생해야 한다")
    void should_ThrowException_When_ResourceNotFound() {
        UUID memberId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.subscribe(memberId, storeId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("구독 정보를 찾아 정상적으로 삭제해야 한다")
    void should_DeleteSubscription_When_SubscriptionExists() {
        UUID memberId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        Subscription subscription = Subscription.builder().build();

        given(subscriptionRepository.findByMemberIdAndStoreId(memberId, storeId))
                .willReturn(Optional.of(subscription));

        subscriptionService.unsubscribe(memberId, storeId);

        verify(subscriptionRepository, times(1)).delete(subscription);
    }

    @Test
    @DisplayName("구독 정보가 없는데 취소하려고 하면 예외가 발생해야 한다")
    void should_ThrowException_When_UnsubscribingNonExistent() {
        UUID memberId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        given(subscriptionRepository.findByMemberIdAndStoreId(memberId, storeId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.unsubscribe(memberId, storeId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SUBSCRIPTION_NOT_FOUND);

        verify(subscriptionRepository, times(0)).delete(any());
    }

    @Test
    @DisplayName("내 구독 목록을 조회하면 DTO로 변환되어 반환되어야 한다")
    void should_ReturnSubscriptionDtoPage_When_SubscriptionsExist() {
        UUID memberId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        Member owner = Member.builder().nickname("사장님").build();
        Store store = Store.builder().id(storeId).title("맛집").member(owner).build();

        Subscription subscription =
                Subscription.builder().id(UUID.randomUUID()).store(store).build();

        Pageable pageable = PageRequest.of(0, 10);
        Slice<Subscription> subSlice = new SliceImpl<>(List.of(subscription));

        given(subscriptionRepository.findAllByMemberId(memberId, pageable)).willReturn(subSlice);

        Slice<SubscriptionDto.SubscriptionResponse> result =
                subscriptionService.getMySubscriptions(memberId, pageable);

        assertThat(result.getContent()).hasSize(1);
        SubscriptionDto.SubscriptionResponse dto = result.getContent().get(0);

        assertThat(dto.getStoreName()).isEqualTo("맛집");
        assertThat(dto.getMemberName()).isEqualTo("사장님");
        assertThat(dto.getStoreUrl()).isEqualTo("/stores/" + storeId);
    }
}
