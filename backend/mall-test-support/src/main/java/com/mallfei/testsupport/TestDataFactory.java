package com.mallfei.testsupport;

import java.time.LocalDateTime;

/**
 * 跨模块通用测试数据工厂。
 *
 * <p>只放通用标识与时间，业务模型对象由各模块测试侧按模块边界构造，避免 test-support 反向依赖业务模块。</p>
 */
public final class TestDataFactory {

    public static final Long USER_ID = 10L;
    public static final Long ADMIN_ID = 1L;
    public static final Long SKU_ID = 1001L;
    public static final Long SPU_ID = 2001L;
    public static final String ORDER_NO = "ORDER-TEST-1001";
    public static final String PAY_ORDER_NO = "PAY-TEST-1001";

    private TestDataFactory() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.of(2026, 1, 1, 10, 0, 0);
    }

    public static LocalDateTime expiredAt() {
        return now().minusMinutes(10);
    }

    public static LocalDateTime futureExpireAt() {
        return now().plusMinutes(30);
    }
}
