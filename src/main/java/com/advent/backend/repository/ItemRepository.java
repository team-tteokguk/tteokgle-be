package com.advent.backend.repository;

import com.advent.backend.entity.Item;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    // 관리자용
    List<Item> findAllByStoreId(UUID storeId);

    // 상점 페이지 별로 아이템 확인하기
    Page<Item> findAllByStoreId(UUID storeId, Pageable pageable);

    // 고객용
    List<Item> findAllByStoreIdAndIsAvailableTrue(UUID storeId);
}
