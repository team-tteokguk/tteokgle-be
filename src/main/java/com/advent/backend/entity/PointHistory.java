package com.advent.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

// 금전 기록
@Entity
@Table(name = "point-history-tbl")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PointHistory extends BaseTimeEntity {

    public enum TradeType {
        CHARGE,
        USE,
        REFUND
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "history_id")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private Member receiver;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private Member sender;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false)
    private TradeType tradeType;

    @Column(name = "amount", nullable = false)
    private Integer amount;
}
