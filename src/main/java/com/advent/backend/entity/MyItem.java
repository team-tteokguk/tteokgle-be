package com.advent.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

// 나의 고명
@Entity
@Table(name = "my_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MyItem extends BaseTimeEntity {
    // 내 고명 ID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 나의 고명의 주인
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 떡국 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tteok_id")
    private MyTteok tteok;

    // 고명 아이템 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    // 컨텐츠 확인 여부
    @Builder.Default
    @Column(columnDefinition = "boolean default false", nullable = false)
    private boolean isRead = false;

    // 고명 올림 여부
    @Builder.Default
    @Column(columnDefinition = "boolean default false", nullable = false)
    private boolean isUsed = false;

    // 나의 고명 x, y, z 위치
    @Column private Float pos_x;
    @Column private Float pos_y;
    @Column private Float pos_z;

    public static MyItem acquire(MyTteok tteok, Item item) {
        return MyItem.builder().tteok(tteok).item(item).build();
    }

    public void place(Float x, Float y, Float z) {
        this.pos_x = x;
        this.pos_y = y;
        this.pos_z = z;
        this.isUsed = true;
    }
}
