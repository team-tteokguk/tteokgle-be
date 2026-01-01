package com.advent.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 고명
@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseTimeEntity {
    // 컨텐츠 타입 (선택 안함, 사진, 유튜브)
    public enum ContentType {
        NONE,
        PHOTO,
        VIDEO
    }

    // 고명 ID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 고명 소속 상점 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    // 가격
    @Column(columnDefinition = "integer default 0", nullable = false)
    private Integer cost = 0;

    // 판매 수량
    @Column(columnDefinition = "integer default 0", nullable = false)
    private Integer count = 0;

    // 고명 이미지
    @Column(nullable = false)
    private String imageUrl;

    // 컨텐츠 타입
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType contentType;

    // 컨텐츠 내용 (이미지 및 유튜브 링크)
    @Column(columnDefinition = "jsonb")
    private String contentData;

    // 본문
    @Column(columnDefinition = "TEXT")
    private String content;

    // 판매 상태
    @Column(columnDefinition = "boolean default true", nullable = false)
    private boolean isAvailable = true;
}
