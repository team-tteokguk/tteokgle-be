package com.advent.backend.repository;

import com.advent.backend.entity.MyItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyItemRepository extends JpaRepository<MyItem, UUID> {
    List<MyItem> findAllByTteok_IdAndIsUsed(UUID tteokId, boolean isUsed);

    List<MyItem> findAllByTteok_Id(UUID tteokId);
}
