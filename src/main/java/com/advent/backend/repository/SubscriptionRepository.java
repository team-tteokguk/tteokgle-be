package com.advent.backend.repository;

import com.advent.backend.entity.Subscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    @EntityGraph(attributePaths = {"store", "store.member"})
    Slice<Subscription> findAllByMemberId(UUID memberId, Pageable pageable);

    boolean existsByMemberIdAndStoreId(UUID memberId, UUID storeId);

    Optional<Subscription> findByMemberIdAndStoreId(UUID memberId, UUID storeId);

    void deleteAllByStoreId(UUID storeId);

    void deleteAllByMemberId(UUID memberId);

    @Query(
            "select s.store.id from Subscription s "
                    + "where s.member.id = :memberId and s.store.id in :storeIds")
    List<UUID> findSubscribedStoreIds(
            @Param("memberId") UUID memberId, @Param("storeIds") List<UUID> storeIds);
}
