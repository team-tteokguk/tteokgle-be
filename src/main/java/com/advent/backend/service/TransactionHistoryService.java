package com.advent.backend.service;

import com.advent.backend.entity.TransactionHistory;
import com.advent.backend.repository.TransactionHistoryRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionHistoryService {
    private final TransactionHistoryRepository transactionHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransactionHistory startHistory(UUID sendId, UUID receiveId) {
        return transactionHistoryRepository.save(
                TransactionHistory.builder()
                        .senderId(sendId)
                        .receiverId(receiveId)
                        .status(TransactionHistory.TransactionStatus.Pending)
                        .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void successHistory(UUID historyId) {
        TransactionHistory history = transactionHistoryRepository.findById(historyId).orElseThrow();
        history.complete();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failHistory(UUID historyId, String errorMessage) {
        TransactionHistory history = transactionHistoryRepository.findById(historyId).orElseThrow();
        history.failed(errorMessage);
    }
}
