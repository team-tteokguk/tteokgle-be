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
     * 방명록 생성
     *
     * @param storeId
     * @param writer
     * @param request
     * @return 생성된 방명록 정보
     */
    @Transactional
    public GuestBookDto.GuestBookResponse createGuestBook(
            UUID storeId, Member writer, GuestBookDto.GuestBookRequest request) {
        Store store =
                storeRepository
                        .findByIdWithMember(storeId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        GuestBook guestBook =
                GuestBook.builder()
                        .store(store)
                        .member(writer)
                        .content(request.getContent())
                        .build();

        GuestBook savedGuestBook = guestBookRepository.save(guestBook);

        if (!store.getMember().getId().equals(writer.getId())) {
            eventPublisher.publishEvent(new CommentEvent(writer, store.getMember(), guestBook));
        }

        return converToResponse(savedGuestBook);
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
     * 방명록 수정 (작성자만 가능)
     *
     * @param guestBookId
     * @param member
     * @param request
     * @return 생성된 방명록 정보
     */
    @Transactional
    public GuestBookDto.GuestBookResponse updateGuestBook(
            UUID guestBookId, Member member, GuestBookDto.GuestBookRequest request) {
        GuestBook guestBook =
                guestBookRepository
                        .findByIdWithMember(guestBookId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.GUESTBOOK_NOT_FOUND));

        validateWriter(guestBook, member);

        guestBook.update(request.getContent());

        return converToResponse(guestBook);
    }

    /** 방명록 삭제(작성자나 상점 주인 가능) */
    @Transactional
    public void deleteGuestBook(UUID guestBookId, Member member) {
        GuestBook guestBook =
                guestBookRepository
                        .findByIdWithMember(guestBookId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.GUESTBOOK_NOT_FOUND));

        validateDeletePermission(guestBook, member);

        guestBookRepository.delete(guestBook);
    }

    /**
     * 권한 체크 (삭제용)
     *
     * @param guestBook
     * @param member
     */
    private void validateDeletePermission(GuestBook guestBook, Member member) {
        UUID requestId = member.getId();
        UUID writerId = guestBook.getMember().getId();
        UUID storeOwnerId = guestBook.getStore().getMember().getId();

        if (!requestId.equals(writerId) && !requestId.equals(storeOwnerId)) {
            throw new BusinessException(ErrorCode.GUESTBOOK_ACCESS_DENIED);
        }
    }

    /**
     * 권한 체크 (수정용)
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
