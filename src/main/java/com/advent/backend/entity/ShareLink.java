package com.advent.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// 공유 링크
@Entity
@Table(name = "share_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShareLink extends BaseTimeEntity {
    // 공유 링크 ID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 상점 ID
    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    // URL에 붙일 토큰
    @Column(nullable = false)
    private String token;
}
