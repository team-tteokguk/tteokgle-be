package com.advent.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

// 나의 떡국
@Entity
@Table(name = "my_tteoks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MyTteok {
    // 나의 떡국 ID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // 유저 ID
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
