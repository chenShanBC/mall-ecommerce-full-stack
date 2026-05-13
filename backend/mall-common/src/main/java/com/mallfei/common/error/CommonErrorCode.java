package com.mallfei.common.error;

public enum CommonErrorCode implements ErrorCode {
    SUCCESS("SUCCESS", "success"),
    BAD_REQUEST("COMMON_400", "请求参数不合法"),
    UNAUTHORIZED("AUTH_401", "未登录或登录已失效"),
    FORBIDDEN("AUTH_403", "无权限访问当前资源"),
    NOT_FOUND("COMMON_404", "资源不存在"),
    CONFLICT("COMMON_409", "请求冲突"),
    VALIDATION_ERROR("COMMON_422", "请求参数校验失败"),
    SYSTEM_ERROR("COMMON_500", "系统异常，请稍后重试");

    private final String code;
    private final String message;

    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
