package com.advent.backend.repository;

import com.advent.backend.entity.GuestBook;
import com.advent.backend.entity.Store;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestBookRepository extends JpaRepository<GuestBook, UUID> {

    Slice<GuestBook> findAllByStore(Store store, Pageable pageable);

    @Query("SELECT g FROM GuestBook g JOIN FETCH g.member WHERE g.store.id = :storeId")
    Slice<GuestBook> findAllByStoreIdWithMember(@Param("storeId") UUID storeId, Pageable pageable);

    @Query("SELECT g FROM GuestBook g JOIN FETCH g.member WHERE g.id = :guestBookId")
    Optional<GuestBook> findByIdWithMember(@Param("guestBookId") UUID guestbookId);

    boolean existsByIdAndMemberId(UUID id, UUID memberId);
}
