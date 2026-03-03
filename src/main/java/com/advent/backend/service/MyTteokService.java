package com.advent.backend.service;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.ItemDto;
import com.advent.backend.entity.Item;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.MyItem;
import com.advent.backend.entity.MyTteok;
import com.advent.backend.repository.MyItemRepository;
import com.advent.backend.repository.MyTteokRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MyTteokService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final float MIN_DISTANCE = 36f;
    private static final float MIN_X = 40f;
    private static final float MAX_X = 320f;
    private static final float MIN_Y = 80f;
    private static final float MAX_Y = 360f;
    private static final float DEFAULT_Z = 1f;
    private static final int RANDOM_CANDIDATE_COUNT = 240;

    private final MyTteokRepository myTteokRepository;
    private final MyItemRepository myItemRepository;

    /**
     * 배치된 고명 리스트 조회
     *
     * @param member
     * @return
     */
    @Transactional(readOnly = true)
    public ItemDto.PlacedItemSliceResponse getPlacedItems(Member member, Pageable pageable) {
        MyTteok myTteok = getMyTteokOrThrow(member);

        Slice<ItemDto.PlacedItemResponse> itemSlice =
                myItemRepository
                        .findAllByTteokIdAndIsUsed(myTteok.getId(), true, pageable)
                        .map(this::toPlacedDto);

        return ItemDto.PlacedItemSliceResponse.from(itemSlice);
    }

    /**
     * 미배치 고명 리스트 조회
     *
     * @param member
     * @return
     */
    @Transactional(readOnly = true)
    public ItemDto.UnplacedItemSliceResponse getUnplacedItems(Member member, Pageable pageable) {
        MyTteok myTteok = getMyTteokOrThrow(member);

        Slice<ItemDto.UnplacedItemResponse> itemSlice =
                myItemRepository
                        .findAllByTteokId(myTteok.getId(), pageable)
                        .map(this::toUnplacedDto);

        return ItemDto.UnplacedItemSliceResponse.from(itemSlice);
    }

    /**
     * 고명 배치 상태 및 좌표 수정
     *
     * @param member
     * @param myItemId
     * @param request
     */
    public void updateItemPlacement(
            Member member, UUID myItemId, ItemDto.ItemPlacementRequest request) {
        MyItem myItem = getMyItemOrThrow(member, myItemId);
        ItemDto.ItemPlacementRequest normalizedRequest =
                request == null ? ItemDto.ItemPlacementRequest.builder().build() : request;

        boolean shouldUse = resolveIsUsed(normalizedRequest, myItem);
        if (!shouldUse) {
            myItem.updatePlacement(false, null, null, null);
            return;
        }

        Position position = pickNonOverlappingPosition(myItem);
        myItem.updatePlacement(true, position.x(), position.y(), position.z());
    }

    /**
     * 고명 컨텐츠 상세 조회
     *
     * @param member
     * @param myItemId
     * @return
     */
    @Transactional(readOnly = true)
    public ItemDto.ItemDetailResponse getItemDetail(Member member, UUID myItemId) {
        MyItem myItem = getMyItemOrThrow(member, myItemId);
        var originalItem = myItem.getItem();

        return ItemDto.ItemDetailResponse.builder()
                .id(myItem.getId())
                .name(originalItem.getName())
                .imageUrl(originalItem.getImageUrl())
                .creatorNickname(extractCreatorNickname(originalItem))
                .itemType(originalItem.getName())
                .contentType(originalItem.getContentType().name())
                .mediaUrl(denormalizeContentData(originalItem.getContentData()))
                .content(originalItem.getContent())
                .isRead(myItem.isRead())
                .build();
    }

    /**
     * 고명 읽음 처리
     *
     * @param member
     * @param myItemId
     */
    public void readItem(Member member, UUID myItemId) {
        MyItem myItem = getMyItemOrThrow(member, myItemId);
        myItem.read();
    }

    /**
     * 배치된 아이템 DTO 변환기
     *
     * @param myItem
     * @return
     */
    private ItemDto.PlacedItemResponse toPlacedDto(MyItem myItem) {
        return ItemDto.PlacedItemResponse.builder()
                .id(myItem.getId())
                .name(myItem.getItem().getName())
                .imageUrl(myItem.getItem().getImageUrl())
                .posX(myItem.getPos_x())
                .posY(myItem.getPos_y())
                .posZ(myItem.getPos_z())
                .isUsed(true)
                .isRead(myItem.isRead())
                .build();
    }

    /**
     * 미배치 아이템 DTO 변환기
     *
     * @param myItem
     * @return
     */
    private ItemDto.UnplacedItemResponse toUnplacedDto(MyItem myItem) {
        return ItemDto.UnplacedItemResponse.builder()
                .id(myItem.getId())
                .name(myItem.getItem().getName())
                .imageUrl(myItem.getItem().getImageUrl())
                .isRead(myItem.isRead())
                .isUsed(myItem.isUsed())
                .build();
    }

    /**
     * 내 떡국 엔티티 가져오기 (없으면 예외 발생)
     *
     * @param member
     * @return
     */
    private MyTteok getMyTteokOrThrow(Member member) {
        return myTteokRepository
                .findByMemberId(member.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TTEOKGUK_NOT_FOUND));
    }

    /**
     * 고명 획득 유저에게 새로운 고명을 생성하여 저장
     *
     * @param member
     * @param item
     * @return
     */
    public MyItem addItem(Member member, Item item) {
        MyTteok myTteok = getMyTteokOrThrow(member);

        MyItem newMyItem =
                MyItem.builder()
                        .member(member)
                        .tteok(myTteok)
                        .item(item)
                        .isUsed(false)
                        .isRead(false)
                        .build();

        return myItemRepository.save(newMyItem);
    }

    /**
     * 내 아이템 엔티티 가져오기 (소유권 체크 포함)
     *
     * @param member
     * @param myItemId
     * @return
     */
    private MyItem getMyItemOrThrow(Member member, UUID myItemId) {
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        MyItem myItem =
                myItemRepository
                        .findById(myItemId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        if (!myItem.getMember().getId().equals(member.getId())) {
            throw new BusinessException(ErrorCode.ITEM_ACCESS_DENIED);
        }
        return myItem;
    }

    private String denormalizeContentData(String contentData) {
        if (contentData == null) {
            return null;
        }
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(contentData);
            if (jsonNode.isTextual()) {
                return jsonNode.asText();
            }
            return contentData;
        } catch (Exception ignored) {
            return contentData;
        }
    }

    private String extractCreatorNickname(Item item) {
        if (item.getStore() == null || item.getStore().getMember() == null) {
            return null;
        }
        return item.getStore().getMember().getNickname();
    }

    private boolean resolveIsUsed(ItemDto.ItemPlacementRequest request, MyItem myItem) {
        if (request.getIsUsed() != null) {
            return request.getIsUsed();
        }

        // If coordinates are provided without explicit isUsed, treat it as "place".
        if (request.getPosX() != null || request.getPosY() != null || request.getPosZ() != null) {
            return true;
        }

        // Default action of this API is "place item".
        return true;
    }

    private Position pickNonOverlappingPosition(MyItem targetItem) {
        UUID tteokId = targetItem.getTteok().getId();
        List<MyItem> placedItems = myItemRepository.findAllByTteokIdAndIsUsed(tteokId, true);
        List<Position> occupied =
                placedItems.stream()
                        .filter(item -> !item.getId().equals(targetItem.getId()))
                        .filter(item -> item.getPos_x() != null && item.getPos_y() != null)
                        .map(
                                item ->
                                        new Position(
                                                item.getPos_x(), item.getPos_y(), item.getPos_z()))
                        .toList();

        Position best = null;
        float bestDistance = -1f;
        for (int i = 0; i < RANDOM_CANDIDATE_COUNT; i++) {
            Position candidate = randomPosition();
            if (isOverlapping(candidate, occupied)) {
                continue;
            }
            float distance = nearestDistanceSquared(candidate, occupied);
            if (distance > bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        if (best != null) {
            return best;
        }

        // Fallback: random tries if grid got saturated.
        for (int i = 0; i < 100; i++) {
            Position random = randomPosition();
            if (!isOverlapping(random, occupied)) {
                return random;
            }
        }

        // Last resort: return any valid position.
        return randomPosition();
    }

    private Position randomPosition() {
        float x = (float) ThreadLocalRandom.current().nextDouble(MIN_X, MAX_X);
        float y = (float) ThreadLocalRandom.current().nextDouble(MIN_Y, MAX_Y);
        return new Position(x, y, DEFAULT_Z);
    }

    private boolean isOverlapping(Position candidate, List<Position> occupied) {
        for (Position p : occupied) {
            float dx = candidate.x() - p.x();
            float dy = candidate.y() - p.y();
            if ((dx * dx + dy * dy) < (MIN_DISTANCE * MIN_DISTANCE)) {
                return true;
            }
        }
        return false;
    }

    private float nearestDistanceSquared(Position candidate, List<Position> occupied) {
        if (occupied.isEmpty()) {
            return Float.MAX_VALUE;
        }

        float minDistance = Float.MAX_VALUE;
        for (Position p : occupied) {
            float dx = candidate.x() - p.x();
            float dy = candidate.y() - p.y();
            float distance = dx * dx + dy * dy;
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    private record Position(float x, float y, float z) {}
}
