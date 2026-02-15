package com.advent.backend.service;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.ItemDto;
import com.advent.backend.dto.StoreDto;
import com.advent.backend.entity.*;
import com.advent.backend.event.PurchaseEvent;
import com.advent.backend.repository.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {
    private final StoreRepository storeRepository;
    private final ItemRepository itemRepository;
    private final MyTteokRepository myTteokRepository;
    private final MyItemRepository myItemRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;

    private final PointService pointService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /** 물건을 구매 */
    @Transactional
    public void purchaseItem(UUID buyerId, UUID itemId) {
        // 1. 구매할 아이템 확인
        Item item =
                itemRepository
                        .findById(itemId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        // 2. 판매자 ID 조회
        UUID sellerId = item.getStore().getMember().getId();

        // 3. 데드락 방지를 위한 락 순서 결정 및 멤버 조회
        UUID firstId = buyerId.compareTo(sellerId) < 0 ? buyerId : sellerId;
        UUID secondId = buyerId.compareTo(sellerId) < 0 ? sellerId : buyerId;

        Member first =
                memberRepository
                        .findByIdWithLock(firstId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Member second =
                (firstId.equals(secondId))
                        ? first
                        : memberRepository
                                .findByIdWithLock(secondId)
                                .orElseThrow(
                                        () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Member buyer = buyerId.equals(firstId) ? first : second;
        Member seller = sellerId.equals(firstId) ? first : second;

        // 4. 구매자의 '나의 떡국' 확인
        MyTteok receiverTteok =
                myTteokRepository
                        .findByMemberId(buyerId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TTEOKGUK_NOT_FOUND));

        // 5. 돈 송금하기
        pointService.transfer(buyer, seller, item.getId(), item.getCost(), item.getName());

        // 6. MyItem 생성 후 내 떡국에 저장
        saveMyItem(buyer, item, receiverTteok);

        // 7. 성공 시 알림 이벤트 발행
        applicationEventPublisher.publishEvent(new PurchaseEvent(buyer, seller, item.getName()));

        // 8. 트랜잭션 성공 기록 생성
        String txId = UUID.randomUUID().toString();

        log.info("[TX_SUCCESS] ID: {}, buyer: {}, seller: {}", txId, buyer.getId(), seller.getId());
    }

    private void saveMyItem(Member buyer, Item item, MyTteok myTteok) {
        MyItem myitem = MyItem.builder().member(buyer).item(item).tteok(myTteok).build();
        myItemRepository.save(myitem);
    }

    /** 상점 정보 조회 */
    @Transactional(readOnly = true)
    public StoreDto.StoreResponse getStoreInfo(UUID storeId) {
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        return StoreDto.StoreResponse.from(store);
    }

    @Transactional(readOnly = true)
    public StoreDto.StoreResponse getMyStoreInfo(UUID memberId) {
        Store store =
                storeRepository
                        .findByMemberId(memberId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        return StoreDto.StoreResponse.from(store);
    }

    /**
     * 특정 상점의 물건을 페이지네이션으로 전달
     *
     * @return ItemDto.StoreItemResponse
     */
    @Transactional(readOnly = true)
    public ItemDto.StoreItemSliceResponse getItems(UUID storeId, Pageable pageable) {
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        Slice<Item> itemSlice = itemRepository.findAllByStoreId(storeId, pageable);
        Slice<ItemDto.StoreItemResponse> mappedSlice =
                itemSlice.map(ItemDto.StoreItemResponse::from);
        long sellingItemCount = itemRepository.countByStoreIdAndIsAvailableTrue(storeId);

        return ItemDto.StoreItemSliceResponse.of(store.getTitle(), sellingItemCount, mappedSlice);
    }

    @Transactional(readOnly = true)
    public ItemDto.StoreItemSliceResponse getMyItems(UUID memberId, Pageable pageable) {
        Store store =
                storeRepository
                        .findByMemberId(memberId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        Slice<Item> itemSlice = itemRepository.findAllByStoreId(store.getId(), pageable);
        Slice<ItemDto.StoreItemResponse> mappedSlice =
                itemSlice.map(ItemDto.StoreItemResponse::from);
        long sellingItemCount = itemRepository.countByStoreIdAndIsAvailableTrue(store.getId());

        return ItemDto.StoreItemSliceResponse.of(store.getTitle(), sellingItemCount, mappedSlice);
    }

    /** 상점 주인이 가판대에 상품을 진열한다 (상품을 등록) */
    @Transactional
    public ItemDto.StoreItemResponse publishItem(UUID storeId, ItemDto.ItemCreateRequest request) {
        validateItemCreateRequest(request);

        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        String imageUrl = request.getImageUrl() == null ? "" : request.getImageUrl();

        Item savedItem =
                itemRepository.save(
                        Item.builder()
                                .store(store)
                                .name(request.getName().trim())
                                .imageUrl(imageUrl)
                                .contentType(request.getContentType())
                                .contentData(request.getMediaUrl())
                                .content(request.getContent())
                                .cost(100) // TODO: 가격 정책
                                .build());

        return ItemDto.StoreItemResponse.from(savedItem);
    }

    private void validateItemCreateRequest(ItemDto.ItemCreateRequest request) {
        if (request == null
                || request.getName() == null
                || request.getName().isBlank()
                || request.getContentType() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String trimmedName = request.getName().trim();
        if (trimmedName.length() > 30) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /** 상점 주인이 판매 중인 물건을 삭제 */
    @Transactional
    public void removeItem(UUID storeId, UUID itemId) {
        Item item =
                itemRepository
                        .findByStoreIdAndId(storeId, itemId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        itemRepository.delete(item);
    }

    @Transactional
    public void updateStoreName(UUID memberId, String storeName) {
        validateStoreName(storeName);

        Store store =
                storeRepository
                        .findByMemberId(memberId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        store.updateTitle(storeName.trim());
    }

    private void validateStoreName(String storeName) {
        if (storeName == null || storeName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_STORE_NAME);
        }

        String trimmed = storeName.trim();
        if (trimmed.length() < 2 || trimmed.length() > 20) {
            throw new BusinessException(ErrorCode.INVALID_STORE_NAME);
        }
    }
}
