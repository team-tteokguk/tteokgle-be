package com.advent.backend.common.error;

import lombok.Getter;

/*
   HTTP 상태 코드 및 서비스 자체 에러 코드를 정의합니다.
*/

@Getter
public enum ErrorCode {
    // 공통 에러
    INVALID_INPUT_VALUE(400, "C001", "올바르지 않은 입력값입니다."),
    METHOD_NOT_ALLOWED(405, "C002", "허용되지 않은 메소드입니다."),
    ENTITY_NOT_FOUND(400, "C003", "데이터를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "C004", "서버 내부에 오류가 발생했습니다."),

    // 금전 거래 에러
    INSUFFICIENT_BALANCE(400, "P001", "포인트 잔액이 부족합니다."),
    SELF_TRANSFER_NOT_ALLOWED(400, "P002", "자기 자신에게 송금할 수 없습니다."),
    INVALID_TRANSFER_AMOUNT(404, "P003", "유효하지 않은 포인트입니다."),

    // 아이템 유효성
    ITEM_NOT_FOUND(409, "I001", "존재하지 않는 아이템입니다."),

    // 사용자 유효성
    MEMBER_NOT_FOUND(404, "M001", "존재하지 않는 사용자입니다."),
    MYTTEOK_NOT_FOUND(404, "M002", "존재하지 않는 떡국입니다."),

    // 상점 유효성
    STORE_NOT_FOUND(404, "M002", "존재하지 않는 상점입니다."),
    SELF_SUBSCRIPTION_NOT_ALLOWED(400, "S002", "본인 상점은 구독할 수 없습니다."),

    // 구독
    ALREADY_SUBSCRIBED(409, "S003", "이미 구독 중인 상점입니다."),
    SUBSCRIPTION_NOT_FOUND(404, "S004", "구독 정보를 찾을 수 없습니다."),

    // 방명록 유효성
    GUESTBOOK_NOT_FOUND(404, "G001", "존재하지 않는 방명록입니다."),
    NOT_GUESTBOOK_WRITER(403, "G002", "작성자만 수정/삭제할 수 있습니다."),

    // 알림 유효성
    NOTIFICATION_NOT_FOUND(404, "N001", "존재하지 않는 알림입니다."),

    // 비지니스 충돌
    DUPLICATE_NICKNAME(409, "M003", "이미 존재하는 닉네임입니다."),

    // 입력값 유효성 (값 자체가 잘못되었을 때 - 400)
    INVALID_NICKNAME_LENGTH(400, "M004", "닉네임 길이가 유효하지 않습니다."),
    RESTRICTED_NICKNAME(400, "M005", "사용할 수 없는 닉네임입니다."),
    INVALID_NICKNAME(400, "M006", "유효하지 않은 닉네임입니다."),

    // 트랜잭션 오류 (유효하지 않은 상태)
    INVALID_TRANSACTION_STATUS(409, "T001", "유효하지 않은 거래입니다."),

    // 리프레시 토큰이 없는 경우
    REFRESHTOKEN_NOT_FOUND(404, "R001", "존재하지 않는 토큰입니다."),
    EXPIRED_TOKEN(404, "R002", "만료된 토큰입니다."),
    INVALID_TOKEN(404, "R003", "유효하지 않는 토큰입니다.");

    private final String code;
    private final String message;
    private final int status;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
