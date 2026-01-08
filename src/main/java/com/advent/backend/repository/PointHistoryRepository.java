package com.advent.backend.repository;

import com.advent.backend.entity.PointHistory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, UUID> {
    // 구매자의 로그 중 가장 최신인 정보 하나를 가져오기
    Optional<PointHistory> findTopBySenderIdOrderByCreatedAtDesc(UUID memberId);

    // 구매자의 로그를 최신순으로 가져오기
    List<PointHistory> findAllBySenderIdOrderByCreatedAtDesc(UUID memberId);

    // 판매자의 로그를 최신순으로 가져오기
    List<PointHistory> findAllByReceiverIdOrderByCreatedAtDesc(UUID memberId);

    // 페이징 처리
    Page<PointHistory> findBySenderIdOrderByCreatedAtDesc(UUID memberId, Pageable pageable);

    // 페이징 처리
    Page<PointHistory> findByReceiverIdOrderByCreatedAtDesc(UUID memberId, Pageable pageable);
}
