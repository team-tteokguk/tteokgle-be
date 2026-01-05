package com.advent.backend.repository;

import com.advent.backend.entity.MyItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyItemRepository extends JpaRepository<MyItem, UUID> {
    List<MyItem> findAllByTteokIdAndIsUsed(UUID tteokId, boolean isUsed);

    List<MyItem> findAllByTteok_Id(UUID tteokId);

    // 멤버 아이디로 해당 멤버가 가지고 있는 고명 리스트 출력
    List<MyItem> findByMemberId(UUID memberId);
}
