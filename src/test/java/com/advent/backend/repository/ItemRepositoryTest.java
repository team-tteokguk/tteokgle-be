package com.advent.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.advent.backend.entity.Item;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.Store;
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
public class ItemRepositoryTest {

    @Autowired private ItemRepository itemRepository;

    @Autowired private TestEntityManager em;

    private Store myStore;
    private Store otherStore;

    @BeforeEach
    void setUp() {
        Member owner =
                Member.builder()
                        .nickname("사장님")
                        .socialId("owner")
                        .socialType(Member.SocialType.GOOGLE)
                        .build();
        em.persist(owner);

        myStore = Store.builder().member(owner).title("대박 고명집").build();
        em.persist(myStore);

        Member sideOwner =
                Member.builder()
                        .nickname("옆집사장")
                        .socialId("side_owner")
                        .socialType(Member.SocialType.GOOGLE)
                        .build();
        em.persist(sideOwner);

        otherStore = Store.builder().member(sideOwner).title("옆집").build();
        em.persist(otherStore);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("특정 상점의 판매 중인 아이템만 조회")
    void should_ReturnAvailableItems_When_StoreIdMatches() {

        Item item1 = Item.create(myStore, "img1.jpg", 5, Item.ContentType.PHOTO, "예쁘죠?");
        em.persist(item1);

        Item item2 = Item.create(myStore, "img2.jpg", 6, Item.ContentType.PHOTO, "구경하고 가세요~!");
        em.persist(item2);

        Item otherItem = Item.create(otherStore, "img3.jpg", 7, Item.ContentType.PHOTO, "옆집 입니다!");
        em.persist(otherItem);

        em.flush();
        em.clear();

        List<Item> result = itemRepository.findAllByStoreIdAndIsAvailableTrue(myStore.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting("imageUrl").containsExactlyInAnyOrder("img1.jpg", "img2.jpg");
        assertThat(result).extracting("cost").contains(5, 6);
    }

    @Test
    @DisplayName("아이템이 없는 상점")
    void should_ReturnEmptyList_When_StoreHasNoItems() {

        List<Item> result = itemRepository.findAllByStoreIdAndIsAvailableTrue(myStore.getId());

        assertThat(result).isEmpty();
    }
}
