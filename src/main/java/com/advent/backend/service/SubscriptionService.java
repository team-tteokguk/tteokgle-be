package com.advent.backend.service;

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
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        if (store.getMember().getId().equals(member.getId())) {
            throw new BusinessException(ErrorCode.SELF_SUBSCRIPTION_NOT_ALLOWED);
        }

        if (subscriptionRepository.existsByMemberIdAndStoreId(memberId, storeId)) {
            throw new BusinessException(ErrorCode.ALREADY_SUBSCRIBED);
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
                        .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

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
