package com.mallfei.testsupport;

/**
 * 集成测试基类。
 *
 * <p>定位：预留给真实链路验证，例如 HTTP 接口 + MySQL + Redis + MQ 的端到端流程。</p>
 *
 * <p>当前项目默认不启用真实集成测试。后续如需落地，应优先使用 Testcontainers 或独立测试环境，
 * 禁止连接开发、生产或个人本地真实业务库，避免污染业务数据。</p>
 */
public abstract class BaseIntegrationTest {
}
