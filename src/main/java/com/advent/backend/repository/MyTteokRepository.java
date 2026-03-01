package com.advent.backend.repository;

import com.advent.backend.entity.MyTteok;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MyTteokRepository extends JpaRepository<MyTteok, UUID> {
    Optional<MyTteok> findByMemberId(UUID memberId);

    boolean existsByMemberId(UUID memberId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MyTteok t WHERE t.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") UUID memberId);
}
