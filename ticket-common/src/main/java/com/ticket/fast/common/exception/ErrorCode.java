package com.ticket.fast.common.exception;


import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "올바르지 않은 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부에 오류가 발생했습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "존재하지 않는 회원입니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M002", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "M003", "비밀번호가 일치하지 않습니다."),
    ADMIN_ADMIRE_ACTION(HttpStatus.FORBIDDEN, "M004", "관리자만 가능합니다."),

    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "티켓 정보를 찾을 수 없습니다."),
    TICKET_SOLD_OUT(HttpStatus.BAD_REQUEST, "T002", "매진된 티켓 입니다."),


    INVALID_TIME_REQUEST(HttpStatus.BAD_REQUEST, "P001", "공연의 시작시간은 종료시간 이전이어야 합니다."),
    SEAT_UNAVAILABLE(HttpStatus.CONFLICT, "P002", "이미 타인에게 선택된 좌석입니다."),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "해당 좌석이 존재하지 않습니다."),
    ;



    private final HttpStatus status;
    private final String code;
    private final String message;

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
    }
