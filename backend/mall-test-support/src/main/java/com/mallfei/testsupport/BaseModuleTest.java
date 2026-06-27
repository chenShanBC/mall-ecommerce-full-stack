package com.mallfei.testsupport;

/**
 * 模块级 Spring 测试基类。
 *
 * <p>定位：验证单个业务模块内 Bean 装配、模块协作流程和关键业务闭环。</p>
 *
 * <p>边界：允许启动 Spring TestContext，但必须 Mock 掉数据库、Redis、MQ、第三方 SDK、文件存储等外部依赖，
 * 不允许连接真实中间件，也不允许写入真实业务数据。</p>
 */
public abstract class BaseModuleTest {
}
