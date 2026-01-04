package com.advent.backend.service;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.entity.*;
import com.advent.backend.repository.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;
    private final MyItemRepository myItemRepository;
    private final MyTteokRepository myTteokRepository;
    private final NotificationRepository notificationRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public void transfer(UUID senderId, UUID receiverId, UUID itemId) {
        // 1. 자기 자신 송금 제한
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.SELF_TRANSFER_NOT_ALLOWED);
        }

        // 2. 데드락 방지를 위한 락 순서 결정 및 멤버 조회
        UUID firstId = senderId.compareTo(receiverId) < 0 ? senderId : receiverId;
        UUID secondId = senderId.compareTo(receiverId) < 0 ? receiverId : senderId;

        memberRepository
                .findByIdWithLock(firstId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        memberRepository
                .findByIdWithLock(secondId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Member sender = memberRepository.findById(senderId).get();
        Member receiver = memberRepository.findById(receiverId).get();

        // 3. 구매자의 나의 떡국 조회
        MyTteok receiverTteok =
                myTteokRepository
                        .findByMember_Id(receiverId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MYTTEOK_NOT_FOUND));

        // 4. 구매할 아이템 조회
        Item item =
                itemRepository
                        .findById(itemId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        Integer itemCost = item.getCost();

        // 5. 송금 금액 유효성 검사
        if (itemCost <= 0) {
            throw new BusinessException(ErrorCode.INVALID_TRANSFER_AMOUNT);
        }

        // 6. 비즈니스 로직 수행 (엔티티 내부 검증 로직 실행)
        sender.decreasePoint(itemCost); // 잔액 부족 시 여기서 예외 발생 -> 자동 롤백
        receiver.addPoint(itemCost);

        // 7. MyItem 생성 후 내 떡국에 저장
        saveMyItem(sender, item, receiverTteok);

        // 8. 거래 내역 저장
        savePointHistory(sender, receiver, item, itemCost, PointHistory.TradeType.CHARGE);

        // 9.  알림 저장
        notificationRepository.save(
                Notification.builder()
                        .member(receiver)
                        .NotificationType(Notification.NotificationType.SALE)
                        .message(sender.getNickname() + "님이 " + item.getContent() + "을 구매하셨습니다.")
                        .build());
    }

    private void saveMyItem(Member buyer, Item item, MyTteok myTteok) {
        MyItem myitem = MyItem.builder().member(buyer).item(item).tteok(myTteok).build();
        myItemRepository.save(myitem);
    }

    private void savePointHistory(
            Member sender, Member receiver, Item item, int amount, PointHistory.TradeType type) {
        PointHistory history =
                PointHistory.builder()
                        .sender(sender)
                        .receiver(receiver)
                        .amount(amount)
                        .tradeType(type)
                        .item(item)
                        .build();

        pointHistoryRepository.save(history);
    }
}
