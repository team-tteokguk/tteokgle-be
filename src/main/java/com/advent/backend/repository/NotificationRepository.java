package com.advent.backend.repository;

import com.advent.backend.entity.Notification;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    // 한번에 알림 읽음 메소드
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = TRUE WHERE n.id = :id")
    void updateNotification(@Param("id") UUID id);
}
