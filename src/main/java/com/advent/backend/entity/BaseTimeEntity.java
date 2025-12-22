package com.advent.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 등록일, 수정일
@Getter
@MappedSuperclass // 부모 클래스를 칼럼으로 포함
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    // 등록일
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 수정일 자동 갱신
    @LastModifiedDate
    private LocalDateTime modifiedAt;
}
