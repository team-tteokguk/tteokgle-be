package com.advent.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;

// 방명록
@Entity
@Table(name = "guestbooks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class GuestBook extends BaseTimeEntity {
    // 방명록 ID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 유저 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(nullable = false, length = 200)
    private String content;

    public static GuestBook write(Member member, Store store, String content) {
        GuestBook guestBook = new GuestBook();
        guestBook.member = member;
        guestBook.store = store;
        guestBook.content = content;
        return guestBook;
    }

    public void update(String content) {
        this.content = content;
    }
}
