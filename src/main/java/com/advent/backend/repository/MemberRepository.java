package com.advent.backend.repository;

import com.advent.backend.entity.Member;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {
    // 소셜 ID와 소셜 Type을 통해 멤버를 조회
    Optional<Member> findBySocialTypeAndSocialId(Member.SocialType socialType, String socialId);

    // 개별 멤버 포인트 업데이트
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findByIdWithLock(@Param("id") UUID id);

    // 특정 멤버들 포인트 업데이트
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Member m SET m.point = m.point + :amount")
    int addPointToMembers(@Param("amount") int amount, @Param("ids") List<UUID> ids);

    // 모든 멤버 포인트 업데이트
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Member m SET m.point = m.point + :amount")
    int addPointToAllMemebers(@Param("amount") int amount);
}
