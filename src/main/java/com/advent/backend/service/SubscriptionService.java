package com.advent.backend.service;

import com.advent.backend.dto.SubscriptionDto;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.Store;
import com.advent.backend.entity.Subscription;
import com.advent.backend.event.SubscribeEvent;
import com.advent.backend.repository.MemberRepository;
import com.advent.backend.repository.StoreRepository;
import com.advent.backend.repository.SubscriptionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void subscribe(UUID memberId, UUID storeId) {
        Member member =
                memberRepository
                        .findById(memberId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상점입니다."));

        if (store.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("본인 상점은 구독할 수 없습니다.");
        }

        if (subscriptionRepository.existsByMemberIdAndStoreId(memberId, storeId)) {
            throw new IllegalArgumentException("이미 구독 중인 상점입니다.");
        }

        Subscription subscription =
                Subscription.builder().member(member).store(store).isNotificated(true).build();

        subscriptionRepository.save(subscription);

        eventPublisher.publishEvent(new SubscribeEvent(member, store.getMember()));
    }

    @Transactional
    public void unsubscribe(UUID memberId, UUID storeId) {
        Subscription subscription =
                subscriptionRepository
                        .findByMemberIdAndStoreId(memberId, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));

        subscriptionRepository.delete(subscription);
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionDto.SubscriptionResponse> getMySubscriptions(
            UUID memberId, Pageable pageable) {
        return subscriptionRepository.findAllByMemberId(memberId, pageable).map(this::convertToDto);
    }

    private SubscriptionDto.SubscriptionResponse convertToDto(Subscription sub) {
        return SubscriptionDto.SubscriptionResponse.builder()
                .id(sub.getId().toString())
                .storeName(sub.getStore().getTitle())
                .storeUrl("/stores/" + sub.getStore().getId())
                .MemberName(sub.getStore().getMember().getNickname())
                .build();
    }
}
