package com.advent.backend.entity;

// 상점

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseTimeEntity {
    // pk (상점 ID)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // fk (상점 주인 ID)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "FK_USER_ID"))
    private Member member;

    // 상점 이름
    @Column(nullable = false)
    private String title;
}
