package com.planit.global.common.exception;

public enum ErrorCode {
    COMMON_001("COMMON_001", "잘못된 요청입니다"),
    COMMON_999("COMMON_999", "서버 오류가 발생했습니다");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
