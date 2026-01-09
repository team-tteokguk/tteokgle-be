package com.advent.backend.repository;

import com.advent.backend.entity.Member;
import com.advent.backend.entity.Notification;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // 내 알림 목록 조회
    List<Notification> findAllByMemberOrderByCreatedAtDesc(Member member);

    // 페이지 단위로 알림 조회
    Page<Notification> findAllByMember(Member member, Pageable pageable);

    // 한번에 알림 읽음 메소드
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = TRUE WHERE n.id = :id")
    void updateNotification(@Param("id") UUID id);
}
