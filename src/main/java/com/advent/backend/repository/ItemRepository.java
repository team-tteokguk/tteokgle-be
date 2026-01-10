package com.advent.backend.repository;

import com.advent.backend.entity.Item;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // 관리자용
    List<Item> findAllByStoreId(UUID storeId);

    // 상점 페이지 별로 아이템 확인하기
    Page<Item> findAllByStoreId(UUID storeId, Pageable pageable);

    // 고객용
    List<Item> findAllByStoreIdAndIsAvailableTrue(UUID storeId);
}
