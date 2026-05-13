package com.mallfei.common.api;

import com.mallfei.common.error.CommonErrorCode;
import com.mallfei.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "ApiResponse", description = "统一接口响应包装")
public record ApiResponse<T>(
        @Schema(description = "是否成功", example = "true")
        boolean success,
        @Schema(description = "业务响应码", example = "SUCCESS")
        String code,
        @Schema(description = "响应消息", example = "success")
        String message,
        @Schema(description = "响应数据")
        T data,
        @Schema(description = "响应时间", example = "2026-05-01T10:00:00")
        LocalDateTime timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, CommonErrorCode.SUCCESS.code(), CommonErrorCode.SUCCESS.message(), data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, CommonErrorCode.SUCCESS.code(), message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.code(), errorCode.message(), null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null, LocalDateTime.now());
    }
}
