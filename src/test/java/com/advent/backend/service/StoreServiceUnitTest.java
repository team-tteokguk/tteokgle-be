package com.advent.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.StoreDto;
import com.advent.backend.entity.Item;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.MyItem;
import com.advent.backend.entity.MyTteok;
import com.advent.backend.entity.Store;
import com.advent.backend.entity.Subscription;
import com.advent.backend.repository.ItemRepository;
import com.advent.backend.repository.MemberRepository;
import com.advent.backend.repository.MyItemRepository;
import com.advent.backend.repository.MyTteokRepository;
import com.advent.backend.repository.StoreRepository;
import com.advent.backend.repository.SubscriptionRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class StoreServiceUnitTest {
    @InjectMocks private StoreService storeService;

    @Mock private StoreRepository storeRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private MyTteokRepository myTteokRepository;
    @Mock private MyItemRepository myItemRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PointService pointService;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @Test
    @DisplayName("상점 정보 조회 응답에는 상점 주인 프로필 이미지가 포함된다")
    void should_IncludeOwnerProfileImage_When_GetStoreInfo() {
        UUID storeId = UUID.randomUUID();
        Member owner =
                Member.builder()
                        .id(UUID.randomUUID())
                        .nickname("사장님")
                        .profileImage("https://cdn.example.com/owner.png")
                        .build();
        Store store = Store.builder().id(storeId).title("내상점").member(owner).build();

        given(storeRepository.findById(storeId)).willReturn(java.util.Optional.of(store));

        StoreDto.StoreResponse result = storeService.getStoreInfo(storeId);

        assertThat(result.getId()).isEqualTo(storeId);
        assertThat(result.getName()).isEqualTo("내상점");
        assertThat(result.getProfileImage()).isEqualTo("https://cdn.example.com/owner.png");
    }

    @Test
    @DisplayName("닉네임/상점명 검색 결과에 판매중 종류 수와 즐겨찾기 여부가 반영된다")
    void should_ReturnStoreSummary_When_SearchStores() {
        UUID me = UUID.randomUUID();
        UUID store1Id = UUID.randomUUID();
        UUID store2Id = UUID.randomUUID();

        Member owner1 =
                Member.builder().id(UUID.randomUUID()).nickname("사장1").profileImage("p1").build();
        Member owner2 =
                Member.builder().id(UUID.randomUUID()).nickname("사장2").profileImage("p2").build();

        Store store1 = Store.builder().id(store1Id).title("고명상점1").member(owner1).build();
        Store store2 = Store.builder().id(store2Id).title("고명상점2").member(owner2).build();

        Pageable pageable = PageRequest.of(0, 10);
        Slice<Store> searchSlice = new SliceImpl<>(List.of(store1, store2), pageable, false);

        given(storeRepository.searchByKeywordExcludingMemberId(me, "고명", pageable))
                .willReturn(searchSlice);
        given(subscriptionRepository.findSubscribedStoreIds(me, List.of(store1Id, store2Id)))
                .willReturn(List.of(store1Id));
        given(itemRepository.countAvailableItemTypesByStoreIds(List.of(store1Id, store2Id)))
                .willReturn(
                        List.<Object[]>of(
                                new Object[] {store1Id, 3L}, new Object[] {store2Id, 1L}));

        Slice<StoreDto.StoreSummaryResponse> result = storeService.searchStores(me, "고명", pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getNickname()).isEqualTo("사장1");
        assertThat(result.getContent().get(0).getStoreName()).isEqualTo("고명상점1");
        assertThat(result.getContent().get(0).getProfileImage()).isEqualTo("p1");
        assertThat(result.getContent().get(0).getSellingItemTypeCount()).isEqualTo(3L);
        assertThat(result.getContent().get(0).isFavorite()).isTrue();
        assertThat(result.getContent().get(1).isFavorite()).isFalse();
    }

    @Test
    @DisplayName("내 즐겨찾기 목록 조회 시 모든 항목의 즐겨찾기 여부는 true이다")
    void should_ReturnFavorites_When_GetMyFavoriteStores() {
        UUID me = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        Member owner =
                Member.builder()
                        .id(UUID.randomUUID())
                        .nickname("사장님")
                        .profileImage("profile")
                        .build();
        Store store = Store.builder().id(storeId).title("떡가게").member(owner).build();
        Subscription subscription =
                Subscription.builder().id(UUID.randomUUID()).member(owner).store(store).build();

        Pageable pageable = PageRequest.of(0, 10);
        Slice<Subscription> subSlice = new SliceImpl<>(List.of(subscription), pageable, false);

        given(subscriptionRepository.findAllByMemberId(me, pageable)).willReturn(subSlice);
        given(itemRepository.countAvailableItemTypesByStoreIds(List.of(storeId)))
                .willReturn(List.<Object[]>of(new Object[] {storeId, 2L}));

        Slice<StoreDto.StoreSummaryResponse> result =
                storeService.getMyFavoriteStores(me, pageable);

        assertThat(result.getContent()).hasSize(1);
        StoreDto.StoreSummaryResponse summary = result.getContent().get(0);
        assertThat(summary.getStoreId()).isEqualTo(storeId);
        assertThat(summary.getNickname()).isEqualTo("사장님");
        assertThat(summary.getStoreName()).isEqualTo("떡가게");
        assertThat(summary.getSellingItemTypeCount()).isEqualTo(2L);
        assertThat(summary.isFavorite()).isTrue();
    }

    @Test
    @DisplayName("구매자가 동일 컨텐츠 고명을 이미 보유하면 구매를 막는다")
    void should_ThrowException_When_BuyerAlreadyOwnsSameContent() {
        UUID buyerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Member buyer = Member.builder().id(buyerId).nickname("구매자").build();
        Store buyerStore = Store.builder().id(UUID.randomUUID()).member(buyer).title("내상점").build();
        Item targetItem =
                Item.builder()
                        .id(itemId)
                        .store(buyerStore)
                        .contentType(Item.ContentType.VIDEO)
                        .contentData("\"https://youtu.be/abc\"")
                        .content("같은 메시지")
                        .quantity(3)
                        .isAvailable(true)
                        .build();

        Item ownedItem =
                Item.builder()
                        .id(UUID.randomUUID())
                        .store(buyerStore)
                        .contentType(Item.ContentType.VIDEO)
                        .contentData("\"https://youtu.be/abc\"")
                        .content("같은 메시지")
                        .build();
        MyItem myItem =
                MyItem.builder().id(UUID.randomUUID()).member(buyer).item(ownedItem).build();

        given(itemRepository.findById(itemId)).willReturn(java.util.Optional.of(targetItem));
        given(memberRepository.findByIdWithLock(buyerId)).willReturn(java.util.Optional.of(buyer));
        given(myTteokRepository.findByMemberId(buyerId))
                .willReturn(
                        java.util.Optional.of(
                                MyTteok.builder().id(UUID.randomUUID()).member(buyer).build()));
        given(myItemRepository.findByMemberId(buyerId)).willReturn(List.of(myItem));

        assertThatThrownBy(() -> storeService.purchaseItem(buyerId, itemId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", com.advent.backend.common.error.ErrorCode.ITEM_ALREADY_OWNED);

        then(pointService)
                .should(never())
                .transfer(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyString());
    }
}
