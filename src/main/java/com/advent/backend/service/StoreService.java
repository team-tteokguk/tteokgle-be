package com.advent.backend.service;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.ItemDto;
import com.advent.backend.dto.StoreDto;
import com.advent.backend.entity.*;
import com.advent.backend.event.PurchaseEvent;
import com.advent.backend.repository.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
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
        if (!item.isAvailable() || item.getQuantity() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION_STATUS);
        }

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
        item.decreaseQuantity();

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

    @Transactional(readOnly = true)
    public Slice<StoreDto.StoreSummaryResponse> searchStores(
            UUID memberId, String keyword, Pageable pageable) {
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }

        Slice<Store> stores =
                storeRepository.searchByKeywordExcludingMemberId(
                        memberId, trimmedKeyword, pageable);
        return toStoreSummarySlice(memberId, stores);
    }

    @Transactional(readOnly = true)
    public Slice<StoreDto.StoreSummaryResponse> getMyFavoriteStores(
            UUID memberId, Pageable pageable) {
        Slice<Subscription> subscriptions =
                subscriptionRepository.findAllByMemberId(memberId, pageable);
        List<Store> stores =
                subscriptions.getContent().stream().map(Subscription::getStore).toList();

        if (stores.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), pageable, subscriptions.hasNext());
        }

        List<StoreDto.StoreSummaryResponse> content =
                buildStoreSummaryResponses(stores, new HashSet<>(extractStoreIds(stores)));
        return new SliceImpl<>(content, pageable, subscriptions.hasNext());
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

        String imageUrl = request.getImageUrl() == null ? null : request.getImageUrl();
        String trimmedName = request.getName().trim();
        int sellCounts = request.getSellCounts() == null ? 1 : request.getSellCounts();
        Item.ContentType contentType =
                request.getContentType() == null ? Item.ContentType.NONE : request.getContentType();

        Item existingItem = findSameContentItem(storeId, trimmedName, imageUrl, request);
        if (existingItem != null) {
            existingItem.addQuantity(sellCounts);
            return ItemDto.StoreItemResponse.from(existingItem);
        }

        Item savedItem =
                itemRepository.save(
                        Item.builder()
                                .store(store)
                                .name(trimmedName)
                                .imageUrl(imageUrl)
                                .contentType(contentType)
                                .contentData(request.getMediaUrl())
                                .content(request.getContent())
                                .quantity(sellCounts)
                                .isAvailable(sellCounts > 0)
                                .cost(100) // TODO: 가격 정책
                                .build());

        return ItemDto.StoreItemResponse.from(savedItem);
    }

    private void validateItemCreateRequest(ItemDto.ItemCreateRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String trimmedName = request.getName().trim();
        if (trimmedName.length() > 30) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (request.getSellCounts() != null && request.getSellCounts() < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private Item findSameContentItem(
            UUID storeId, String itemName, String imageUrl, ItemDto.ItemCreateRequest request) {
        List<Item> existingItems = itemRepository.findAllByStoreId(storeId);

        return existingItems.stream()
                .filter(item -> Objects.equals(item.getName(), itemName))
                .filter(item -> item.getContentType() == request.getContentType())
                .filter(item -> Objects.equals(item.getImageUrl(), imageUrl))
                .filter(item -> Objects.equals(item.getContentData(), request.getMediaUrl()))
                .filter(item -> Objects.equals(item.getContent(), request.getContent()))
                .findFirst()
                .orElse(null);
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

    private Slice<StoreDto.StoreSummaryResponse> toStoreSummarySlice(
            UUID memberId, Slice<Store> stores) {
        List<Store> content = stores.getContent();
        if (content.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), stores.getPageable(), stores.hasNext());
        }

        List<UUID> storeIds = extractStoreIds(content);
        Set<UUID> subscribedStoreIds =
                new HashSet<>(subscriptionRepository.findSubscribedStoreIds(memberId, storeIds));
        List<StoreDto.StoreSummaryResponse> responses =
                buildStoreSummaryResponses(content, subscribedStoreIds);

        return new SliceImpl<>(responses, stores.getPageable(), stores.hasNext());
    }

    private List<StoreDto.StoreSummaryResponse> buildStoreSummaryResponses(
            List<Store> stores, Set<UUID> subscribedStoreIds) {
        List<UUID> storeIds = extractStoreIds(stores);
        Map<UUID, Long> sellingItemTypeCounts = countSellingItemTypes(storeIds);

        return stores.stream()
                .map(
                        store ->
                                StoreDto.StoreSummaryResponse.builder()
                                        .storeId(store.getId())
                                        .nickname(store.getMember().getNickname())
                                        .storeName(store.getTitle())
                                        .profileImage(store.getMember().getProfileImage())
                                        .sellingItemTypeCount(
                                                sellingItemTypeCounts.getOrDefault(
                                                        store.getId(), 0L))
                                        .favorite(subscribedStoreIds.contains(store.getId()))
                                        .build())
                .toList();
    }

    private List<UUID> extractStoreIds(List<Store> stores) {
        return stores.stream().map(Store::getId).toList();
    }

    private Map<UUID, Long> countSellingItemTypes(List<UUID> storeIds) {
        if (storeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> rows = itemRepository.countAvailableItemTypesByStoreIds(storeIds);
        Map<UUID, Long> countMap = new HashMap<>();
        for (Object[] row : rows) {
            countMap.put((UUID) row[0], (Long) row[1]);
        }
        return countMap;
    }
}
