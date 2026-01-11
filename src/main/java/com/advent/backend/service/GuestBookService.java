package com.advent.backend.service;

import com.advent.backend.common.error.ErrorCode;
import com.advent.backend.common.error.exception.BusinessException;
import com.advent.backend.dto.GuestBookDto;
import com.advent.backend.entity.GuestBook;
import com.advent.backend.entity.Member;
import com.advent.backend.entity.Store;
import com.advent.backend.event.CommentEvent;
import com.advent.backend.repository.GuestBookRepository;
import com.advent.backend.repository.MemberRepository;
import com.advent.backend.repository.StoreRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuestBookService {

    private final GuestBookRepository guestBookRepository;
    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 방명록 등록
     *
     * @param storeId
     * @param writer
     * @param request
     */
    @Transactional
    public void createGuestBook(
            UUID storeId, Member writer, GuestBookDto.GuestBookRequest request) {
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        GuestBook guestBook =
                GuestBook.builder()
                        .store(store)
                        .member(writer)
                        .content(request.getContent())
                        .build();

        guestBookRepository.save(guestBook);

        if (!store.getMember().getId().equals(writer.getId())) {
            eventPublisher.publishEvent(new CommentEvent(writer, store.getMember(), guestBook));
        }
    }

    /**
     * 방명록 조회
     *
     * @param storeId
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Slice<GuestBookDto.GuestBookResponse> getGuestBooks(UUID storeId, Pageable pageable) {
        return guestBookRepository
                .findAllByStoreIdWithMember(storeId, pageable)
                .map(this::converToResponse);
    }

    /**
     * 방명록 수정
     *
     * @param guestBookId
     * @param member
     * @param request
     */
    @Transactional
    public void updateGuestBook(
            UUID guestBookId, Member member, GuestBookDto.GuestBookRequest request) {
        GuestBook guestBook =
                guestBookRepository
                        .findById(guestBookId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.GUESTBOOK_NOT_FOUND));

        validateWriter(guestBook, member);

        guestBook.update(request.getContent());
    }

    /** 방명록 삭제 */
    @Transactional
    public void deleteGuestBook(UUID guestBookId, Member member) {
        GuestBook guestBook =
                guestBookRepository
                        .findById(guestBookId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.GUESTBOOK_NOT_FOUND));

        validateWriter(guestBook, member);

        guestBookRepository.delete(guestBook);
    }

    /**
     * 권한 체크
     *
     * @param guestBook
     * @param member
     */
    private void validateWriter(GuestBook guestBook, Member member) {
        if (!guestBook.getMember().getId().equals(member.getId())) {
            throw new BusinessException(ErrorCode.NOT_GUESTBOOK_WRITER);
        }
    }

    /**
     * Entity -> DTO 변환
     *
     * @param guestBook
     * @return
     */
    private GuestBookDto.GuestBookResponse converToResponse(GuestBook guestBook) {
        return GuestBookDto.GuestBookResponse.builder()
                .id(guestBook.getId().toString())
                .writerId(guestBook.getMember().getId().toString())
                .writerNickname(guestBook.getMember().getNickname())
                .content(guestBook.getContent())
                .createdAt(guestBook.getCreatedAt())
                .build();
    }
}
