package com.advent.backend.repository;

import com.advent.backend.entity.GuestBook;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuestBookRepository extends JpaRepository<GuestBook, UUID> {

    @Query(
            value = "SELECT g FROM GuestBook g JOIN FETCH g.member WHERE g.store.id = :storeId",
            countQuery = "SELECT count(g) FROM GuestBook g WHERE g.store.id = :storeId")
    Page<GuestBook> findAllByStoreIdWithMember(@Param("storeId") UUID storeId, Pageable pageable);

    @Query("SELECT g FROM GuestBook g JOIN FETCH g.member WHERE g.id = :guestBookId")
    Optional<GuestBook> findByIdWithMember(@Param("guestBookId") UUID guestBookId);

    boolean existsByIdAndMemberId(UUID id, UUID memberId);
}
