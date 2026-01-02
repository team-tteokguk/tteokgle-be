package com.advent.backend.repository;

import com.advent.backend.entity.Item;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    // 관리자용
    List<Item> findAllByStoreId(UUID storeId);

    // 고객용
    List<Item> findAllByStoreIdAndIsAvailableTrue(UUID storeId);
}
