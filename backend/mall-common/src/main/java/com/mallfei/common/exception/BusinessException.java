package com.mallfei.common.exception;

import com.mallfei.common.error.CommonErrorCode;
import com.mallfei.common.error.ErrorCode;

public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.code = errorCode.code();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.code();
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(CommonErrorCode.BAD_REQUEST, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(CommonErrorCode.FORBIDDEN, message);
    }
}
