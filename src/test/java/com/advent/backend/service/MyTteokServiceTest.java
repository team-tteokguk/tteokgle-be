package com.advent.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.ItemDto;
import com.advent.backend.entity.Item;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.MyItem;
import com.advent.backend.entity.MyTteok;
import com.advent.backend.repository.MyItemRepository;
import com.advent.backend.repository.MyTteokRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyTteokService 단위 테스트")
class MyTteokServiceTest {

    @InjectMocks private MyTteokService myTteokService;

    @Mock private MyTteokRepository myTteokRepository;

    @Mock private MyItemRepository myItemRepository;

    private Member member;
    private MyTteok myTteok;
    private Item item;
    private MyItem myItem;
    private UUID memberId;
    private UUID myTteokId;
    private UUID myItemId;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        myTteokId = UUID.randomUUID();
        myItemId = UUID.randomUUID();

        member = Member.builder().id(memberId).nickname("테스트유저").build();

        myTteok = MyTteok.builder().id(myTteokId).member(member).build();

        item =
                Item.builder()
                        .id(UUID.randomUUID())
                        .name("황금계란")
                        .imageUrl("gold_egg.png")
                        .contentType(Item.ContentType.PHOTO)
                        .build();

        myItem =
                MyItem.builder()
                        .id(myItemId)
                        .member(member)
                        .tteok(myTteok)
                        .item(item)
                        .isUsed(false)
                        .isRead(false)
                        .build();
    }

    @Nested
    @DisplayName("고명 획득 (addItem)")
    class AddItemTest {

        @Test
        @DisplayName("유저가 고명을 획득하면 내 떡국에 미사용/안읽음 상태로 저장된다")
        void addItem_Success() {
            given(myTteokRepository.findByMemberId(memberId)).willReturn(Optional.of(myTteok));
            given(myItemRepository.save(any(MyItem.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            MyItem savedItem = myTteokService.addItem(member, item);

            ArgumentCaptor<MyItem> captor = ArgumentCaptor.forClass(MyItem.class);
            then(myItemRepository).should().save(captor.capture());

            MyItem captured = captor.getValue();
            assertThat(captured.getMember()).isEqualTo(member);
            assertThat(captured.getItem()).isEqualTo(item);
            assertThat(captured.isUsed()).isFalse();
            assertThat(captured.isRead()).isFalse();

            assertThat(savedItem).isEqualTo(captured);
        }

        @Test
        @DisplayName("떡국이 없는 유저가 고명을 획득하려 하면 예외를 던진다")
        void addItem_NoTteok_ThrowsException() {
            given(myTteokRepository.findByMemberId(memberId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> myTteokService.addItem(member, item))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TTEOKGUK_NOT_FOUND);

            then(myItemRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("고명 조회 (getPlacedItems, getUnplacedItems)")
    class GetItemsTest {

        @Test
        @DisplayName("배치된 고명 리스트를 조회한다")
        void getPlacedItems_Success() {
            MyItem placedItem =
                    MyItem.builder()
                            .id(UUID.randomUUID())
                            .member(member)
                            .tteok(myTteok)
                            .item(item)
                            .pos_x(10.5f)
                            .build();

            given(myTteokRepository.findByMemberId(memberId)).willReturn(Optional.of(myTteok));
            given(myItemRepository.findAllByTteokIdAndIsUsed(myTteokId, true))
                    .willReturn(List.of(placedItem));

            List<ItemDto.PlacedItemResponse> result = myTteokService.getPlacedItems(member);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("황금계란");
            assertThat(result.get(0).getPosX()).isEqualTo(10.5f);
            assertThat(result.get(0).isUsed()).isTrue();
        }

        @Test
        @DisplayName("미배치 고명 리스트를 조회한다")
        void getUnplacedItems_Success() {
            given(myTteokRepository.findByMemberId(memberId)).willReturn(Optional.of(myTteok));
            given(myItemRepository.findAllByTteokIdAndIsUsed(myTteokId, false))
                    .willReturn(List.of(myItem));

            List<ItemDto.UnplacedItemResponse> result = myTteokService.getUnplacedItems(member);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("황금계란");
            assertThat(result.get(0).isRead()).isFalse();
        }
    }

    @Nested
    @DisplayName("고명 배치 수정 (updateItemPlacement)")
    class UpdateItemPlacementTest {

        @Test
        @DisplayName("자신의 고명 위치와 상태를 수정한다")
        void updateItemPlacement_ByOwner_Success() {
            ItemDto.ItemPlacementRequest request =
                    new ItemDto.ItemPlacementRequest(true, 1.0f, 2.0f, 3.0f);

            given(myItemRepository.findById(myItemId)).willReturn(Optional.of(myItem));

            myTteokService.updateItemPlacement(member, myItemId, request);

            assertThat(myItem.isUsed()).isTrue();
            assertThat(myItem.getPos_x()).isEqualTo(1.0f);
            assertThat(myItem.getPos_y()).isEqualTo(2.0f);

            then(myItemRepository).should().findById(myItemId);
        }

        @Test
        @DisplayName("다른 사람의 고명을 수정하려 하면 예외를 던진다")
        void updateItemPlacement_ByStranger_ThrowsException() {
            Member stranger = Member.builder().id(UUID.randomUUID()).nickname("해커").build();
            ItemDto.ItemPlacementRequest request =
                    new ItemDto.ItemPlacementRequest(true, 1.0f, 2.0f, 3.0f);

            given(myItemRepository.findById(myItemId)).willReturn(Optional.of(myItem));

            assertThatThrownBy(
                            () -> myTteokService.updateItemPlacement(stranger, myItemId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ITEM_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("고명 상세 조회 및 읽음 처리")
    class ItemDetailAndReadTest {

        @Test
        @DisplayName("고명 상세 정보를 조회한다")
        void getItemDetail_Success() {
            given(myItemRepository.findById(myItemId)).willReturn(Optional.of(myItem));

            ItemDto.ItemDetailResponse response = myTteokService.getItemDetail(member, myItemId);

            assertThat(response.getId()).isEqualTo(myItemId);
            assertThat(response.getName()).isEqualTo("황금계란");
            assertThat(response.getContentType()).isEqualTo("PHOTO");
        }

        @Test
        @DisplayName("고명을 읽음 처리한다")
        void readItem_Success() {
            given(myItemRepository.findById(myItemId)).willReturn(Optional.of(myItem));

            myTteokService.readItem(member, myItemId);

            assertThat(myItem.isRead()).isTrue();
        }
    }
}
