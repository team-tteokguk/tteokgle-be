package com.advent.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.GuestBookDto;
import com.advent.backend.entity.GuestBook;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.Store;
import com.advent.backend.event.CommentEvent;
import com.advent.backend.repository.GuestBookRepository;
import com.advent.backend.repository.StoreRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuestBookService 단위 테스트")
class GuestBookServiceTest {

    @InjectMocks private GuestBookService guestBookService;

    @Mock private GuestBookRepository guestBookRepository;

    @Mock private StoreRepository storeRepository;

    @Mock private ApplicationEventPublisher eventPublisher;

    private UUID storeId;
    private UUID ownerId;
    private UUID guestId;
    private Member owner;
    private Member guest;
    private Store store;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        guestId = UUID.randomUUID();

        owner = Member.builder().id(ownerId).nickname("상점주인").build();

        guest = Member.builder().id(guestId).nickname("방문손님").build();

        store = Store.builder().id(storeId).member(owner).build();
    }

    @Nested
    @DisplayName("방명록 등록")
    class CreateGuestBookTest {

        @Test
        @DisplayName("손님이 방명록을 작성하면 저장하고 상점 주인에게 알림을 발송한다")
        void createGuestBook_ByGuest_SavesAndPublishesEvent() {
            String content = "멋진 상점이네요!";
            GuestBookDto.GuestBookRequest request = new GuestBookDto.GuestBookRequest(content);

            UUID savedGuestBookId = UUID.randomUUID();
            GuestBook savedGuestBook =
                    GuestBook.builder()
                            .id(savedGuestBookId)
                            .store(store)
                            .member(guest)
                            .content(content)
                            .createdAt(LocalDateTime.now())
                            .build();

            given(storeRepository.findByIdWithMember(storeId)).willReturn(Optional.of(store));
            given(guestBookRepository.save(any(GuestBook.class))).willReturn(savedGuestBook);

            GuestBookDto.GuestBookResponse response =
                    guestBookService.createGuestBook(storeId, guest, request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(savedGuestBookId.toString());
            assertThat(response.getWriterId()).isEqualTo(guestId.toString());
            assertThat(response.getWriterNickname()).isEqualTo("방문손님");
            assertThat(response.getContent()).isEqualTo(content);
            assertThat(response.getCreatedAt()).isNotNull();

            ArgumentCaptor<GuestBook> guestBookCaptor = ArgumentCaptor.forClass(GuestBook.class);
            then(guestBookRepository).should().save(guestBookCaptor.capture());

            GuestBook saved = guestBookCaptor.getValue();
            assertThat(saved.getMember().getId()).isEqualTo(guestId);
            assertThat(saved.getContent()).isEqualTo(content);

            ArgumentCaptor<CommentEvent> eventCaptor = ArgumentCaptor.forClass(CommentEvent.class);
            then(eventPublisher).should().publishEvent(eventCaptor.capture());

            CommentEvent event = eventCaptor.getValue();
            assertThat(event.commenter()).isEqualTo(guest);
            assertThat(event.owner()).isEqualTo(owner);
            assertThat(event.guestBook().getContent()).isEqualTo(content);
        }

        @Test
        @DisplayName("상점 주인이 자신의 방명록에 글을 쓰면 알림을 발송하지 않는다")
        void createGuestBook_ByOwner_DoesNotPublishEvent() {
            String content = "공지사항";
            GuestBookDto.GuestBookRequest request = new GuestBookDto.GuestBookRequest(content);

            UUID savedGuestBookId = UUID.randomUUID();
            GuestBook savedGuestBook =
                    GuestBook.builder()
                            .id(savedGuestBookId)
                            .store(store)
                            .member(owner)
                            .content(content)
                            .createdAt(LocalDateTime.now())
                            .build();

            given(storeRepository.findByIdWithMember(storeId)).willReturn(Optional.of(store));
            given(guestBookRepository.save(any(GuestBook.class))).willReturn(savedGuestBook);

            GuestBookDto.GuestBookResponse response =
                    guestBookService.createGuestBook(storeId, owner, request);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo(content);
            assertThat(response.getWriterId()).isEqualTo(ownerId.toString());

            then(guestBookRepository).should().save(any(GuestBook.class));
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("존재하지 않는 상점에 방명록 작성 시도 시 예외를 던진다")
        void createGuestBook_NonExistentStore_ThrowsException() {
            UUID invalidStoreId = UUID.randomUUID();
            GuestBookDto.GuestBookRequest request = new GuestBookDto.GuestBookRequest("내용");

            given(storeRepository.findByIdWithMember(invalidStoreId)).willReturn(Optional.empty());

            assertThatThrownBy(
                            () -> guestBookService.createGuestBook(invalidStoreId, guest, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STORE_NOT_FOUND);

            then(guestBookRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("방명록 조회")
    class GetGuestBooksTest {

        @Test
        @DisplayName("상점의 방명록 목록을 페이징하여 조회한다")
        void getGuestBooks_WithValidStore_ReturnsPagedResults() {
            GuestBook guestBook1 =
                    GuestBook.builder()
                            .id(UUID.randomUUID())
                            .member(guest)
                            .content("첫번째 방명록")
                            .createdAt(LocalDateTime.now().minusDays(1))
                            .build();

            GuestBook guestBook2 =
                    GuestBook.builder()
                            .id(UUID.randomUUID())
                            .member(owner)
                            .content("두번째 방명록")
                            .createdAt(LocalDateTime.now())
                            .build();

            Pageable pageable = PageRequest.of(0, 10);
            Slice<GuestBook> guestBookSlice =
                    new SliceImpl<>(Arrays.asList(guestBook1, guestBook2), pageable, true);

            given(guestBookRepository.findAllByStoreIdWithMember(storeId, pageable))
                    .willReturn(guestBookSlice);

            Slice<GuestBookDto.GuestBookResponse> result =
                    guestBookService.getGuestBooks(storeId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.hasNext()).isTrue();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getContent()).isEqualTo("첫번째 방명록");
            assertThat(result.getContent().get(0).getWriterNickname()).isEqualTo("방문손님");
            assertThat(result.getContent().get(1).getContent()).isEqualTo("두번째 방명록");

            then(guestBookRepository).should().findAllByStoreIdWithMember(storeId, pageable);
        }

        @Test
        @DisplayName("방명록이 없는 상점은 빈 페이지를 반환한다")
        void getGuestBooks_EmptyStore_ReturnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Slice<GuestBook> emptyPage = Page.empty(pageable);

            given(guestBookRepository.findAllByStoreIdWithMember(storeId, pageable))
                    .willReturn(emptyPage);

            Slice<GuestBookDto.GuestBookResponse> result =
                    guestBookService.getGuestBooks(storeId, pageable);

            assertThat(result).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("방명록 수정")
    class UpdateGuestBookTest {

        private UUID guestBookId;
        private GuestBook guestBook;

        @BeforeEach
        void setUp() {
            guestBookId = UUID.randomUUID();
            guestBook =
                    GuestBook.builder()
                            .id(guestBookId)
                            .member(guest)
                            .content("원본 내용")
                            .createdAt(LocalDateTime.now())
                            .build();
        }

        @Test
        @DisplayName("작성자가 자신의 방명록을 수정하면 내용이 변경된다")
        void updateGuestBook_ByAuthor_UpdatesContent() {
            String updatedContent = "수정된 내용입니다";
            GuestBookDto.GuestBookRequest request =
                    new GuestBookDto.GuestBookRequest(updatedContent);

            given(guestBookRepository.findByIdWithMember(guestBookId))
                    .willReturn(Optional.of(guestBook));

            guestBookService.updateGuestBook(guestBookId, guest, request);

            assertThat(guestBook.getContent()).isEqualTo(updatedContent);
            then(guestBookRepository).should().findByIdWithMember(guestBookId);
        }

        @Test
        @DisplayName("다른 사용자가 방명록을 수정하려고 하면 예외를 던진다")
        void updateGuestBook_ByNonAuthor_ThrowsException() {
            Member stranger = Member.builder().id(UUID.randomUUID()).nickname("제3자").build();

            GuestBookDto.GuestBookRequest request = new GuestBookDto.GuestBookRequest("해킹시도");

            given(guestBookRepository.findByIdWithMember(guestBookId))
                    .willReturn(Optional.of(guestBook));

            assertThatThrownBy(
                            () -> guestBookService.updateGuestBook(guestBookId, stranger, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_GUESTBOOK_WRITER);

            assertThat(guestBook.getContent()).isEqualTo("원본 내용");
        }

        @Test
        @DisplayName("존재하지 않는 방명록을 수정하려고 하면 예외를 던진다")
        void updateGuestBook_NonExistent_ThrowsException() {
            UUID invalidId = UUID.randomUUID();
            GuestBookDto.GuestBookRequest request = new GuestBookDto.GuestBookRequest("수정");

            given(guestBookRepository.findByIdWithMember(invalidId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> guestBookService.updateGuestBook(invalidId, guest, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("방명록 삭제")
    class DeleteGuestBookTest {

        private UUID guestBookId;
        private GuestBook guestBook;

        @BeforeEach
        void setUp() {
            guestBookId = UUID.randomUUID();
            guestBook =
                    GuestBook.builder()
                            .id(guestBookId)
                            .member(guest)
                            .content("삭제될 내용")
                            .createdAt(LocalDateTime.now())
                            .build();
        }

        @Test
        @DisplayName("작성자가 자신의 방명록을 삭제할 수 있다")
        void deleteGuestBook_ByAuthor_DeletesSuccessfully() {
            given(guestBookRepository.findByIdWithMember(guestBookId))
                    .willReturn(Optional.of(guestBook));

            guestBookService.deleteGuestBook(guestBookId, guest);

            then(guestBookRepository).should().delete(guestBook);
        }

        @Test
        @DisplayName("다른 사용자가 방명록을 삭제하려고 하면 예외를 던진다")
        void deleteGuestBook_ByNonAuthor_ThrowsException() {
            Member stranger = Member.builder().id(UUID.randomUUID()).nickname("제3자").build();

            given(guestBookRepository.findByIdWithMember(guestBookId))
                    .willReturn(Optional.of(guestBook));

            assertThatThrownBy(() -> guestBookService.deleteGuestBook(guestBookId, stranger))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.GUESTBOOK_ACCESS_DENIED);

            then(guestBookRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않는 방명록을 삭제하려고 하면 예외를 던진다")
        void deleteGuestBook_NonExistent_ThrowsException() {
            UUID invalidId = UUID.randomUUID();

            given(guestBookRepository.findByIdWithMember(invalidId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> guestBookService.deleteGuestBook(invalidId, guest))
                    .isInstanceOf(BusinessException.class);

            then(guestBookRepository).should(never()).delete(any());
        }
    }
}
