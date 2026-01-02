package com.advent.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 나의 고명
@Entity
@Table(name = "my_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MyItem {
    // 내 고명 ID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 떡국 ID
    @ManyToOne
    @JoinColumn(name = "tteok_id")
    private MyTteok tteok;

    // 고명 아이템 ID
    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    // 컨텐츠 확인 여부
    @Column(columnDefinition = "boolean default false", nullable = false)
    private boolean isRead = false;

    // 고명 올림 여부
    @Column(columnDefinition = "boolean default false", nullable = false)
    private boolean isUsed = false;

    // 나의 고명 x, y, z 위치
    @Column private Float pos_x;
    @Column private Float pos_y;
    @Column private Float pos_z;

    public static MyItem acquire(MyTteok tteok, Item item) {
        MyItem myItem = new MyItem();
        myItem.tteok = tteok;
        myItem.item = item;
        myItem.isUsed = false;
        myItem.isRead = false;
        return myItem;
    }
}
