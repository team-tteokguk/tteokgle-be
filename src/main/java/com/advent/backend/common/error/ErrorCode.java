package com.advent.backend.common.error;

import lombok.Getter;

/*
   HTTP 상태 코드 및 서비스 자체 에러 코드를 정의합니다.
*/

@Getter
public enum ErrorCode {
    // 1. 시스템 및 인증
    // 1.1. 공통
    INVALID_INPUT_VALUE(400, "C001", "올바르지 않은 입력값입니다."),
    METHOD_NOT_ALLOWED(405, "C002", "허용되지 않은 메소드입니다."),
    ENTITY_NOT_FOUND(404, "C003", "데이터를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "C004", "서버 내부에 오류가 발생했습니다."),

    // 1.2. 인증
    INVALID_TOKEN(401, "A001", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(401, "A002", "만료된 토큰입니다."),
    TOKEN_MALFORMED(401, "A003", "손상된 토큰입니다."),
    TOKEN_UNSUPPORTED(401, "A004", "지원하지 않는 토큰입니다."),
    TOKEN_GENERATION_FAILED(500, "A005", "토큰 생성에 실패했습니다."),

    // 1.3. 리프레시 토큰
    INVAILD_REFRESH_TOKEN(401, "R001", "유요하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(401, "R002", "존재하지 않는 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(401, "R003", "만료된 리프레시 토큰입니다."),

    // 2. 회원 및 프로필
    // 2.1. 사용자
    MEMBER_NOT_FOUND(404, "M001", "존재하지 않는 사용자입니다."),

    // 2.2 닉네임
    INVALID_NICKNAME_LENGTH(400, "NI001", "닉네임 길이가 유효하지 않습니다."),
    RESTRICTED_NICKNAME(400, "NI002", "사용할 수 없는 닉네임입니다."),
    INVALID_NICKNAME(400, "NI003", "유효하지 않은 닉네임입니다."),
    DUPLICATE_NICKNAME(409, "NI004", "이미 존재하는 닉네임입니다."),

    // 3. 금전 거래
    // 3.1. 포인트
    INSUFFICIENT_BALANCE(400, "P001", "포인트 잔액이 부족합니다."),
    SELF_TRANSFER_NOT_ALLOWED(400, "P002", "자기 자신에게 송금할 수 없습니다."),
    INVALID_TRANSFER_AMOUNT(404, "P003", "유효하지 않은 포인트입니다."),

    // 3.2. 거래
    INVALID_TRANSACTION_STATUS(409, "T001", "유효하지 않은 거래입니다."),

    // 4. 핵심 서비스
    // 4.1. 아이템
    ITEM_NOT_FOUND(409, "I001", "존재하지 않는 아이템입니다."),
    ITEM_ACCESS_DENIED(403, "I002", "해당 아이템에 대한 권한이 없습니다."),
    ITEM_ALREADY_OWNED(409, "I003", "이미 보유한 고명입니다."),

    // 4.2. 떡국
    TTEOKGUK_NOT_FOUND(404, "TK001", "나의 떡국이 존재하지 않습니다."),

    // 4.3. 상점
    STORE_NOT_FOUND(404, "S001", "존재하지 않는 상점입니다."),
    SELF_SUBSCRIPTION_NOT_ALLOWED(400, "S002", "본인 상점은 구독할 수 없습니다."),
    INVALID_STORE_NAME(400, "S005", "상점명은 2자 이상 20자 이하여야 합니다."),

    // 4.4. 구독
    ALREADY_SUBSCRIBED(409, "S003", "이미 구독 중인 상점입니다."),
    SUBSCRIPTION_NOT_FOUND(404, "S004", "구독 정보를 찾을 수 없습니다."),

    // 4.5. 방명록
    GUESTBOOK_NOT_FOUND(404, "G001", "존재하지 않는 방명록입니다."),
    NOT_GUESTBOOK_WRITER(403, "G002", "작성자만 수정/삭제할 수 있습니다."),
    GUESTBOOK_ACCESS_DENIED(403, "G003", "해당 방명록에 대한 권한이 없습니다."),

    // 4.6. 알림
    NOTIFICATION_NOT_FOUND(404, "N001", "존재하지 않는 알림입니다.");

    private final String code;
    private final String message;
    private final int status;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
