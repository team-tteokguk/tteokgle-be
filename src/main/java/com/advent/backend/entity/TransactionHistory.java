package com.advent.backend.entity;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "transactionHistorys")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TransactionHistory extends BaseTimeEntity {
    public enum TransactionStatus {
        Pending, // 대기
        Success, // 완료
        Failed, // 실패
    }

    // 트랜잭션 기록 ID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column private UUID senderId;

    @Column private UUID receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column private String errorMessage;

    public void complete() {
        if (this.status != TransactionStatus.Pending) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }
        this.status = TransactionStatus.Success;
    }

    public void failed(String errorMessage) {
        if (this.status != TransactionStatus.Pending) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }
        this.status = TransactionStatus.Failed;
        this.errorMessage = errorMessage;
    }
}
