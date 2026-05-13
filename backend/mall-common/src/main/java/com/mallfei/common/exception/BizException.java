package com.mallfei.common.exception;

@Deprecated(forRemoval = false)
public class BizException extends BusinessException {

    public BizException(Integer code, String message) {
        super(String.valueOf(code), message);
    }
}
