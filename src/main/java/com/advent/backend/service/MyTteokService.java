package com.advent.backend.service;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.ItemDto;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.MyItem;
import com.advent.backend.entity.MyTteok;
import com.advent.backend.repository.MyItemRepository;
import com.advent.backend.repository.MyTteokRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MyTteokService {

    private final MyTteokRepository myTteokRepository;
    private final MyItemRepository myItemRepository;

    /**
     * 배치된 고명 리스트 조회
     *
     * @param member
     * @return
     */
    @Transactional(readOnly = true)
    public List<ItemDto.PlacedItemResponse> getPlacedItems(Member member) {
        MyTteok myTteok = getMyTteokOrThrow(member);

        return myItemRepository.findAllByTteokIdAndIsUsed(myTteok.getId(), true).stream()
                .map(this::toPlacedDto)
                .collect(Collectors.toList());
    }

    /**
     * 미배치 고명 리스트 조회
     *
     * @param member
     * @return
     */
    @Transactional(readOnly = true)
    public List<ItemDto.UnplacedItemResponse> getUnplacedItems(Member member) {
        MyTteok myTteok = getMyTteokOrThrow(member);

        return myItemRepository.findAllByTteokIdAndIsUsed(myTteok.getId(), false).stream()
                .map(this::toUnplacedDto)
                .collect(Collectors.toList());
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

        myItem.updatePlacement(
                request.isUsed(), request.getPosX(), request.getPosY(), request.getPosZ());
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
                .contentType(originalItem.getContentType().name())
                .mediaUrl(originalItem.getContentData())
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
                .isUsed(true)
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
     * 내 아이템 엔티티 가져오기 (소유권 체크 포함)
     *
     * @param member
     * @param myItemId
     * @return
     */
    private MyItem getMyItemOrThrow(Member member, UUID myItemId) {
        MyItem myItem =
                myItemRepository
                        .findById(myItemId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        if (!myItem.getMember().getId().equals(member.getId())) {
            throw new BusinessException(ErrorCode.ITEM_ACCESS_DENIED);
        }
        return myItem;
    }
}
