package com.advent.backend.entity;

// 상점

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Store extends BaseTimeEntity {
    // pk (상점 ID)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // fk (상점 주인 ID)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(name = "FK_USER_ID"))
    private Member member;

    // 상점 이름
    @Column(nullable = false)
    private String title;

    public static Store create(Member member, String title) {
        return Store.builder().member(member).title(title).build();
    }
}
