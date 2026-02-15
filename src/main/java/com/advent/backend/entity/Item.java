package com.advent.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// 고명
@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
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

    @Column(nullable = false, length = 30)
    private String name;

    // 가격
    @Column(nullable = false)
    private Integer cost;

    // 판매 수량
    @Builder.Default
    @Column(columnDefinition = "integer default 0", nullable = false)
    private Integer quantity = 0;

    // 고명 이미지
    @Column(nullable = false)
    private String imageUrl;

    // 컨텐츠 타입
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType contentType;

    // 컨텐츠 내용 (이미지 및 유튜브 링크)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String contentData;

    // 본문
    @Column(columnDefinition = "TEXT")
    private String content;

    // 판매 상태
    @Builder.Default
    @Column(columnDefinition = "boolean default true", nullable = false)
    private boolean isAvailable = true;

    public static Item create(
            Store store, String imageUrl, Integer cost, ContentType contentType, String content) {
        return Item.builder()
                .store(store)
                .name("이름")
                .imageUrl(imageUrl)
                .cost(cost)
                .contentType(contentType)
                .content(content)
                .build();
    }

    public void addQuantity(int quantity) {
        this.quantity += quantity;
    }
}
