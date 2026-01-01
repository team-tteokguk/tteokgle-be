package com.advent.backend.repository;

import com.advent.backend.entity.PointHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, UUID> {
    // 특정 유저의 로그를 최신순으로 가져오기
    List<PointHistory> findAllByMemberIdOrderByCreatedAtDesc(UUID memberId);

    // 페이징 처리
    Page<PointHistory> findByMemberIdOrderByCreatedAtDesc(UUID memberId, Pageable pageable);
}
