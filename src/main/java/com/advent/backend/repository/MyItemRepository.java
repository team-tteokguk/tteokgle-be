package com.advent.backend.repository;

import com.advent.backend.entity.MyItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MyItemRepository extends JpaRepository<MyItem, UUID> {
    List<MyItem> findAllByTteokIdAndIsUsed(UUID tteokId, boolean isUsed);

    Slice<MyItem> findAllByTteokIdAndIsUsed(UUID tteokId, boolean isUsed, Pageable pageable);

    Slice<MyItem> findAllByTteokId(UUID tteokId, Pageable pageable);

    List<MyItem> findAllByTteok_Id(UUID tteokId);

    // 멤버 아이디로 해당 멤버가 가지고 있는 고명 리스트 출력
    List<MyItem> findByMemberId(UUID memberId);

    @Query(
            "SELECT mi.item.id FROM MyItem mi WHERE mi.member.id = :memberId AND mi.item.id IN :itemIds")
    List<UUID> findOwnedItemIdsByMemberIdAndItemIds(
            @Param("memberId") UUID memberId, @Param("itemIds") List<UUID> itemIds);

    void deleteAllByMemberId(UUID memberId);

    void deleteAllByTteokId(UUID tteokId);

    void deleteAllByItem_Store_Id(UUID storeId);
}
