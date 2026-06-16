-- 认证会话安全事件日志表
-- 记录登录成功、后登踢前登、主动退出、账号禁用强制下线等审计事件。
-- 设计原则：只追加、不参与业务主链路事务，日志写入失败不应影响登录/退出流程。

USE `mall_fei`;

CREATE TABLE IF NOT EXISTS `auth_session_event_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `principal_id` bigint NOT NULL COMMENT '主体ID：用户ID或管理员ID',
  `identity_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '身份类型：USER/ADMIN',
  `account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录账号，展示时建议脱敏',
  `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备类型：USER_H5/ADMIN_WEB等',
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件类型：LOGIN_SUCCESS/LOGIN_REPLACED/LOGOUT/DISABLED_FORCE_LOGOUT',
  `result` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '事件结果',
  `login_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Sa-Token 登录ID，如 USER:1、ADMIN:1',
  `token_digest` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Token SHA-256 摘要，不存储明文Token',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'unknown' COMMENT '客户端IP',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端User-Agent',
  `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '事件说明',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_auth_session_event_principal` (`identity_type`, `principal_id`, `created_at` DESC) USING BTREE,
  INDEX `idx_auth_session_event_type_time` (`event_type`, `created_at` DESC) USING BTREE,
  INDEX `idx_auth_session_event_login_id` (`login_id`) USING BTREE,
  INDEX `idx_auth_session_event_ip_time` (`ip`, `created_at` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '认证会话安全事件日志表' ROW_FORMAT = DYNAMIC;
