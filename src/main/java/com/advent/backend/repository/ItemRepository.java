package com.advent.backend.repository;

import com.advent.backend.entity.Item;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    // item 조회 시 판매자 아이디까지 한번에 조회하기
    @Query(
            "select i from Item i "
                    + "join fetch i.store s "
                    + "join fetch s.member m "
                    + "where i.id = :itemId")
    Optional<Item> findItemWithSeller(@Param("itemId") UUID itemId);

    // 아이템 조회 시 상점 존재 여부 + 상점과 아이템 매치 여부까지 확인
    Optional<Item> findByStoreIdAndId(UUID storeId, UUID itemId);

    // 관리자용
    List<Item> findAllByStoreId(UUID storeId);

    // 상점 페이지 별로 아이템 확인하기
    Slice<Item> findAllByStoreId(UUID storeId, Pageable pageable);

    void deleteAllByStoreId(UUID storeId);

    long countByStoreIdAndIsAvailableTrue(UUID storeId);

    @Query(
            "select i.store.id, count(i) "
                    + "from Item i "
                    + "where i.store.id in :storeIds and i.isAvailable = true "
                    + "group by i.store.id")
    List<Object[]> countAvailableItemTypesByStoreIds(@Param("storeIds") List<UUID> storeIds);

    // 고객용
    List<Item> findAllByStoreIdAndIsAvailableTrue(UUID storeId);
}
