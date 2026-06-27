package com.mallfei.testsupport;

import com.mallfei.common.error.CommonErrorCode;
import com.mallfei.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 企业级单元测试基类。
 *
 * <p>规范：纯领域逻辑不启动 Spring 容器；业务异常同时断言错误码和核心文案；
 * 仓储、缓存、MQ、第三方 SDK 等外部副作用必须显式 verify。</p>
 */
public abstract class BaseUnitTest {

    protected BusinessException assertBadRequest(Throwable throwable, String messagePart) {
        return assertBusinessException(throwable, CommonErrorCode.BAD_REQUEST.code(), messagePart);
    }

    protected BusinessException assertForbidden(Throwable throwable, String messagePart) {
        return assertBusinessException(throwable, CommonErrorCode.FORBIDDEN.code(), messagePart);
    }

    protected BusinessException assertBusinessException(Throwable throwable, String expectedCode, String messagePart) {
        assertThat(throwable)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(messagePart);
        BusinessException exception = (BusinessException) throwable;
        assertThat(exception.getCode()).isEqualTo(expectedCode);
        return exception;
    }
}
