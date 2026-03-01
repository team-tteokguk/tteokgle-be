package com.advent.backend.repository;

import com.advent.backend.entity.Member;
import com.advent.backend.entity.Store;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

    // 기본 조회

    // 특정 회원 상점 조회
    Optional<Store> findByMember(Member member);

    // 회원 ID로 상점 조회
    Optional<Store> findByMemberId(UUID memberId);

    // 상점 ID로 조회하면서 회원 정보 함께 가져오기
    @Query("SELECT s FROM Store s LEFT JOIN FETCH s.member WHERE s.id = :storeId")
    Optional<Store> findByIdWithMember(@Param("storeId") UUID storeId);

    // 상점 이름으로 검색
    Optional<Store> findByTitle(String title);

    // 상점 이름으로 부분 검색 (포함 검색)
    List<Store> findByTitleContaining(String keyword);

    // 멤버 이름으로 상점 검색
    Optional<Store> findByMemberNickname(String name);

    // 멤버 이름 부분 검색
    List<Store> findByMemberNicknameContaining(String keyword);

    // 존재 여부 확인

    // 특정 회원이 상점을 가지고 있는지 확인
    boolean existsByMember(Member member);

    // 회원 ID로 상점 존재 여부 확인
    boolean existsByMemberId(UUID memberId);

    // 상점 ID 회원 ID 일치 확인 (권한 체크)
    boolean existsByIdAndMemberId(UUID storeId, UUID memberId);

    // 통계 및 분석

    // 특정 기간 이후 생성된 상점 개수
    @Query("SELECT COUNT(s) FROM Store s WHERE s.createdAt >= :date")
    long countByCreatedAtAfter(@Param("date") java.time.LocalDateTime date);

    List<Store> findTop10ByOrderByCreatedAtDesc();

    @Query(
            "select s from Store s "
                    + "join fetch s.member m "
                    + "where m.id <> :memberId "
                    + "and (lower(s.title) like lower(concat('%', :keyword, '%')) "
                    + "or lower(m.nickname) like lower(concat('%', :keyword, '%')))")
    Slice<Store> searchByKeywordExcludingMemberId(
            @Param("memberId") UUID memberId, @Param("keyword") String keyword, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Store s WHERE s.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") UUID memberId);
}
