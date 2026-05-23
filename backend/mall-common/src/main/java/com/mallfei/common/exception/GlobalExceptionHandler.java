package com.mallfei.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.mallfei.common.api.ApiResponse;
import com.mallfei.common.error.CommonErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        log.warn("Business exception, code={}, message={}", exception.getCode(), exception.getMessage());
        String code = normalizeBusinessCode(exception.getCode());
        return ApiResponse.failure(code, safeBusinessMessage(exception));
    }

    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<Void> handleNotLoginException(NotLoginException exception) {
        return ApiResponse.failure(CommonErrorCode.UNAUTHORIZED.code(), CommonErrorCode.UNAUTHORIZED.message());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : CommonErrorCode.VALIDATION_ERROR.message();
        return ApiResponse.failure(CommonErrorCode.VALIDATION_ERROR.code(), safeClientMessage(message, CommonErrorCode.VALIDATION_ERROR.message()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        return ApiResponse.failure(CommonErrorCode.VALIDATION_ERROR.code(), CommonErrorCode.VALIDATION_ERROR.message());
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ApiResponse<Void> handleBadRequestException(Exception exception) {
        log.warn("Bad request: {}", exception.getMessage());
        return ApiResponse.failure(CommonErrorCode.BAD_REQUEST.code(), CommonErrorCode.BAD_REQUEST.message());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        return ApiResponse.failure(CommonErrorCode.BAD_REQUEST.code(), "请求方式不正确");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResponse<Void> handleNoHandlerFoundException(NoHandlerFoundException exception) {
        return ApiResponse.failure(CommonErrorCode.NOT_FOUND.code(), CommonErrorCode.NOT_FOUND.message());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ApiResponse.failure(CommonErrorCode.SYSTEM_ERROR.code(), CommonErrorCode.SYSTEM_ERROR.message());
    }

    private String safeBusinessMessage(BusinessException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return CommonErrorCode.BAD_REQUEST.message();
        }
        return safeClientMessage(exception.getMessage(), CommonErrorCode.BAD_REQUEST.message());
    }

    private String normalizeBusinessCode(String code) {
        if (CommonErrorCode.FORBIDDEN.code().equals(code)) {
            return CommonErrorCode.FORBIDDEN.code();
        }
        if (CommonErrorCode.UNAUTHORIZED.code().equals(code)) {
            return CommonErrorCode.UNAUTHORIZED.code();
        }
        if (code == null || code.isBlank()) {
            return CommonErrorCode.BAD_REQUEST.code();
        }
        return code;
    }

    private String safeClientMessage(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        String lowerCaseMessage = message.toLowerCase();
        if (lowerCaseMessage.contains("exception")
                || lowerCaseMessage.contains("trace")
                || lowerCaseMessage.contains("sql")
                || lowerCaseMessage.contains("jdbc")
                || lowerCaseMessage.contains("redis")
                || lowerCaseMessage.contains("rabbit")
                || lowerCaseMessage.contains("java.")
                || lowerCaseMessage.contains("org.springframework")
                || lowerCaseMessage.contains("com.mallfei")) {
            return fallback;
        }
        return message;
    }
}
