package com.advent.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

// 금전 기록
@Entity
@Table(name = "point_historys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PointHistory extends BaseTimeEntity {

    public enum TradeType {
        CHARGE,
        USE,
        REFUND,
        EVENT_REWARD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Member receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false)
    private TradeType tradeType;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    public static PointHistory create(
            Member receiver, Member sender, Item item, TradeType tradeType, Integer amount) {
        return PointHistory.builder()
                .receiver(receiver)
                .sender(sender)
                .item(item)
                .tradeType(tradeType)
                .amount(amount)
                .build();
    }
}
