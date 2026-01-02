package com.advent.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.advent.backend.entity.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MyItemRepositoryTest {

    @Autowired private MyItemRepository myItemRepository;

    @Autowired private TestEntityManager em;

    private MyTteok myTteok;
    private Item salesItem;

    @BeforeEach
    void SetUp() {
        Member owner =
                Member.builder()
                        .nickname("주인")
                        .socialId("owner_kakao")
                        .socialType(Member.SocialType.KAKAO)
                        .build();
        em.persist(owner);

        Store store = Store.builder().member(owner).title("고명고명").build();
        em.persist(store);

        salesItem = Item.register(store, "img123.jpg", 5, Item.ContentType.PHOTO, "최고급 고명");
        em.persist(salesItem);

        Member player =
                Member.builder()
                        .nickname("외요123")
                        .socialId("oeyo_kakao")
                        .socialType(Member.SocialType.KAKAO)
                        .build();
        em.persist(player);

        myTteok = MyTteok.create(player);
        em.persist(myTteok);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("인벤토리 고명 조회")
    void should_ReturnUnplacedItems_When_IsUsedIsFalse() {
        MyItem inventoryItem1 = MyItem.acquire(myTteok, salesItem);
        MyItem inventoryItem2 = MyItem.acquire(myTteok, salesItem);
        em.persist(inventoryItem1);
        em.persist(inventoryItem2);

        MyItem placedItem = MyItem.acquire(myTteok, salesItem);
        placedItem.place(10f, 10f, 10f); // 배치함!
        em.persist(placedItem);

        em.flush();
        em.clear();

        List<MyItem> result = myItemRepository.findAllByTteok_IdAndIsUsed(myTteok.getId(), false);

        assertThat(result).hasSize(2);
        assertThat(result).extracting("isUsed").containsOnly(false);
    }

    @Test
    @DisplayName("배치된 고명 조회")
    void should_ReturnPlacedItems_When_IsUsedIsTrue() {
        MyItem placedItem = MyItem.acquire(myTteok, salesItem);
        placedItem.place(12.5f, 30.0f, 5.0f);
        em.persist(placedItem);

        MyItem inventoryItem = MyItem.acquire(myTteok, salesItem);
        em.persist(inventoryItem);

        em.flush();
        em.clear();

        List<MyItem> result = myItemRepository.findAllByTteok_IdAndIsUsed(myTteok.getId(), true);

        assertThat(result).hasSize(1);

        MyItem found = result.get(0);
        assertThat(found.isUsed()).isTrue();
        assertThat(found.getPos_x()).isEqualTo(12.5f);
        assertThat(found.getItem().getContent()).isEqualTo("최고급 고명");
    }

    @Test
    @DisplayName("다른 사람 떡국 고명 조회되지 않아야 함")
    void should_NotReturnOtherUsersItems_When_QueryingMyTteok() {
        MyItem myItem = MyItem.acquire(myTteok, salesItem);
        em.persist(myItem);

        Member stranger =
                Member.builder()
                        .nickname("이방인")
                        .socialId("stranger")
                        .socialType(Member.SocialType.KAKAO)
                        .build();
        em.persist(stranger);

        MyTteok otherTteok = MyTteok.create(stranger);
        em.persist(otherTteok);

        MyItem otherItem = MyItem.acquire(otherTteok, salesItem);
        em.persist(otherItem);

        em.flush();
        em.clear();

        List<MyItem> result = myItemRepository.findAllByTteok_IdAndIsUsed(myTteok.getId(), false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTteok().getId()).isEqualTo(myTteok.getId());
    }
}
