package com.advent.backend.repository;

import com.advent.backend.entity.Subscription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    /**
     * 페이지네이션 만큼 구독 기록 조회
     *
     * @param memberId
     * @param pageable
     * @return
     */
    Slice<Subscription> findAllByMemberId(UUID memberId, Pageable pageable);

    boolean existsByMemberIdAndStoreId(UUID memberId, UUID storeId);

    Optional<Subscription> findByMemberIdAndStoreId(UUID memberId, UUID storeId);
}
