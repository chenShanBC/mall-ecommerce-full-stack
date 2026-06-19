/*
 Navicat Premium Dump SQL

 Source Server         : docker_mallfei
 Source Server Type    : MySQL
 Source Server Version : 80409 (8.4.9)
 Source Host           : localhost:3307
 Source Schema         : mall_fei

 Target Server Type    : MySQL
 Target Server Version : 80409 (8.4.9)
 File Encoding         : 65001

 Date: 19/06/2026 14:21:48
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin_operation_config
-- ----------------------------
DROP TABLE IF EXISTS `admin_operation_config`;
CREATE TABLE `admin_operation_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置值',
  `config_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置分组',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置说明',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '后台运营轻量配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of admin_operation_config
-- ----------------------------
INSERT INTO `admin_operation_config` VALUES (1, 'PRODUCT_HOT_SALES_THRESHOLD', '100', 'PRODUCT_SALES', '商品近30天热销阈值', '2026-06-07 01:20:06', '2026-06-15 19:53:17');
INSERT INTO `admin_operation_config` VALUES (2, 'PRODUCT_LOW_SALES_THRESHOLD', '1', 'PRODUCT_SALES', '商品近30天低销阈值', '2026-06-07 01:20:06', '2026-06-15 19:53:17');

-- ----------------------------
-- Table structure for aftersale_order
-- ----------------------------
DROP TABLE IF EXISTS `aftersale_order`;
CREATE TABLE `aftersale_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '售后单ID',
  `aftersale_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '售后单号',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `aftersale_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '售后类型',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '售后状态',
  `origin_order_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '申请售后前订单状态',
  `refund_amount_cent` bigint NULL DEFAULT NULL COMMENT '退款金额，单位分',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请原因',
  `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '驳回原因',
  `refund_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联退款单号',
  `fail_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '退款失败原因',
  `version` int NOT NULL DEFAULT 0 COMMENT '版本号',
  `reviewed_at` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_aftersale_order_no`(`aftersale_no` ASC) USING BTREE,
  INDEX `idx_aftersale_order_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_aftersale_order_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_aftersale_order_status`(`status` ASC) USING BTREE,
  INDEX `idx_aftersale_order_refund_no`(`refund_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '售后单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of aftersale_order
-- ----------------------------
INSERT INTO `aftersale_order` VALUES (1, 'AFTRCT202606100008', 'RCT202606100008', 1, 'ONLY_REFUND', 'REFUND_SUCCESS', 'PAID', 18000, '对账退款测试', NULL, 'RFDRCT202606100008', NULL, 1, '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 15:18:18');
INSERT INTO `aftersale_order` VALUES (2, 'AFT1781076032228AB7729', 'ORD1781076001361016574', 1, 'ONLY_REFUND', 'REJECTED', 'PAID', 21000, '...', '商家审核驳回', NULL, NULL, 1, '2026-06-10 15:20:43', '2026-06-10 15:20:32', '2026-06-10 15:20:43');
INSERT INTO `aftersale_order` VALUES (3, 'AFT17810780633198F8972', 'RCT202606100009', 1, 'ONLY_REFUND', 'REFUND_SUCCESS', 'PAID', 19000, '...', NULL, 'ORF1781078073650ED3D88', NULL, 3, '2026-06-10 15:54:34', '2026-06-10 15:54:23', '2026-06-10 15:54:47');
INSERT INTO `aftersale_order` VALUES (4, 'AFT1781200699204F54809', 'ORD17812006903496B061E', 1, 'ONLY_REFUND', 'REFUND_PROCESSING', 'PAID', 21000, '...', NULL, 'ORF17812007111280C0DE2', NULL, 2, '2026-06-12 01:58:31', '2026-06-12 01:58:19', '2026-06-12 01:58:31');
INSERT INTO `aftersale_order` VALUES (5, 'AFT1781526648822FD054D', 'ORD1781526648573A839F8', 1, 'ONLY_REFUND', 'REFUND_PROCESSING', 'PAID', 10000, '商品与描述不符', NULL, 'ORF1781526648822FD054D', NULL, 2, '2026-06-15 20:30:48', '2026-06-15 20:30:48', '2026-06-15 20:30:49');
INSERT INTO `aftersale_order` VALUES (6, 'AFT178152672284164F2E8', 'ORD178152672263850B15C', 1, 'ONLY_REFUND', 'REFUND_PROCESSING', 'PAID', 10000, '商品与描述不符', NULL, 'ORF178152672284164F2E8', NULL, 2, '2026-06-15 20:32:02', '2026-06-15 20:32:02', '2026-06-15 20:32:03');
INSERT INTO `aftersale_order` VALUES (7, 'AFT1781528098611D0788A', 'ORD178152809766513D1B9', 1, 'ONLY_REFUND', 'REFUND_PROCESSING', 'PAID', 10000, 'P4 refund', NULL, 'ORF1781528098611D0788A', NULL, 2, '2026-06-15 20:54:58', '2026-06-15 20:54:58', '2026-06-15 20:54:59');
INSERT INTO `aftersale_order` VALUES (8, 'AFT1781528713716FCFAFF', 'ORD1781528713367F731BD', 1, 'ONLY_REFUND', 'REFUND_PROCESSING', 'PAID', 10000, '商品与描述不符', NULL, 'ORF1781528713716FCFAFF', NULL, 2, '2026-06-15 21:05:13', '2026-06-15 21:05:13', '2026-06-15 21:05:14');
INSERT INTO `aftersale_order` VALUES (9, 'AFT178154211823272684B', 'ORD17815421181506A2B36', 1, 'ONLY_REFUND', 'PENDING_REVIEW', 'COMPLETED', 10000, '商品收到后发现有轻微瑕疵，申请审核退款', NULL, NULL, NULL, 1, '2026-06-16 00:48:39', '2026-06-16 00:48:38', '2026-06-16 00:48:38');
INSERT INTO `aftersale_order` VALUES (10, 'AFT17815425522336B688D', 'ORD17815425521398BF4BA', 1, 'ONLY_REFUND', 'PENDING_REVIEW', 'COMPLETED', 10000, '包装破损影响二次销售，申请审核退款', NULL, NULL, NULL, 1, '2026-06-16 00:55:53', '2026-06-16 00:55:52', '2026-06-16 00:55:52');

-- ----------------------------
-- Table structure for auth_session_event_log
-- ----------------------------
DROP TABLE IF EXISTS `auth_session_event_log`;
CREATE TABLE `auth_session_event_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `principal_id` bigint NOT NULL COMMENT '主体ID：用户ID或管理员ID',
  `identity_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '身份类型：USER/ADMIN',
  `account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录账号，展示时建议脱敏',
  `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备类型：USER_H5/ADMIN_WEB等',
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件类型：LOGIN_SUCCESS/LOGIN_REPLACED/LOGOUT/DISABLED_FORCE_LOGOUT',
  `result` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '事件结果',
  `login_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Sa-Token登录ID，如USER:1、ADMIN:1',
  `token_digest` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Token SHA-256摘要，不存储明文Token',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'unknown' COMMENT '客户端IP',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端User-Agent',
  `message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '事件说明',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_auth_session_event_principal`(`identity_type` ASC, `principal_id` ASC, `created_at` DESC) USING BTREE,
  INDEX `idx_auth_session_event_type_time`(`event_type` ASC, `created_at` DESC) USING BTREE,
  INDEX `idx_auth_session_event_login_id`(`login_id` ASC) USING BTREE,
  INDEX `idx_auth_session_event_ip_time`(`ip` ASC, `created_at` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 65 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '认证会话安全事件日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of auth_session_event_log
-- ----------------------------
INSERT INTO `auth_session_event_log` VALUES (1, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', 'demo-admin-token', '127.0.0.1', 'Mozilla/5.0', '管理员登录日志', '2026-06-10 10:00:00');
INSERT INTO `auth_session_event_log` VALUES (2, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', 'demo-user-token', '127.0.0.1', 'Mozilla/5.0 Mobile', '用户登录日志', '2026-06-10 10:00:00');
INSERT INTO `auth_session_event_log` VALUES (3, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', 'a49fa85f9b2f1353795416a7934515a59991c71f7040bd6b58c0933b5f46b227', '127.0.0.1', 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36 Edg/149.0.0.0', '账号登录成功', '2026-06-10 15:19:33');
INSERT INTO `auth_session_event_log` VALUES (4, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-10 15:46:23');
INSERT INTO `auth_session_event_log` VALUES (5, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', 'd462b2165104edf1b92046b8c1c1d2bf5bbb6bb2827fbe6bf667392b5738adaa', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-10 15:46:23');
INSERT INTO `auth_session_event_log` VALUES (6, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-10 15:46:30');
INSERT INTO `auth_session_event_log` VALUES (7, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', 'ae73f73eb8637c9629218c6f78e9d20959ade3fb933c379fc026c4a0457914c7', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-10 15:46:30');
INSERT INTO `auth_session_event_log` VALUES (8, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', 'da388e1c22b03b7888009ef8f81eb2d2a3f43300f6d9fdd078e9ff38c5ccf9b0', '127.0.0.1', 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36 Edg/149.0.0.0', '账号登录成功', '2026-06-10 16:30:28');
INSERT INTO `auth_session_event_log` VALUES (9, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-10 18:37:40');
INSERT INTO `auth_session_event_log` VALUES (10, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', 'efce77aeb22e18b27d3020b34eb9c5ab2e6ec2ef9d6914d1a324dd0a9706187d', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-10 18:37:40');
INSERT INTO `auth_session_event_log` VALUES (11, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-11 15:43:00');
INSERT INTO `auth_session_event_log` VALUES (12, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', '73a0ee33f4a504ae35683f41c03e56b7d1502a6ee5e56ef3a11654eb97857dde', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-11 15:43:00');
INSERT INTO `auth_session_event_log` VALUES (13, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-11 16:29:17');
INSERT INTO `auth_session_event_log` VALUES (14, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', 'ddd734e6ef03671fe0d374216b2b3b0a36bff3b3f7609e508138a34532c56221', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-11 16:29:17');
INSERT INTO `auth_session_event_log` VALUES (15, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-11 22:11:03');
INSERT INTO `auth_session_event_log` VALUES (16, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', '91bedbae5debe247e8e311793a434f33a03cdfa5687c84f73ed7f0904f2ff9b8', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-11 22:11:03');
INSERT INTO `auth_session_event_log` VALUES (17, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-12 01:57:50');
INSERT INTO `auth_session_event_log` VALUES (18, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', 'd91b1afec38fb1581c406aca7f2a670f54fcdd79575a0d80abfbcad82cfe6835', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-12 01:57:50');
INSERT INTO `auth_session_event_log` VALUES (19, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', '688b679864c386e490b053d20c1ba2dcc50a5a24f8f6861b17143a527511e63f', '127.0.0.1', 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36 Edg/149.0.0.0', '账号登录成功', '2026-06-12 01:58:09');
INSERT INTO `auth_session_event_log` VALUES (20, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-13 19:56:26');
INSERT INTO `auth_session_event_log` VALUES (21, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', 'd0912b623629b239a10878e630645118f00689ec61595ca2a2b6c03c99710448', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-13 19:56:26');
INSERT INTO `auth_session_event_log` VALUES (22, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-14 20:49:42');
INSERT INTO `auth_session_event_log` VALUES (23, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', '9a668406dc56766f380034be559b85d4d56b621d50ec49f0ba37f44e0a377e17', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-14 20:49:42');
INSERT INTO `auth_session_event_log` VALUES (24, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', '39b4e357e6ea809770efbfc1ff4515a54308e34a9b20fbe6cec31e78e643f242', '127.0.0.1', 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36 Edg/149.0.0.0', '账号登录成功', '2026-06-15 15:46:54');
INSERT INTO `auth_session_event_log` VALUES (25, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', '9d0e4332945094d0e1ce47b333106d6440849503df2b846f6abe007cd743766d', '127.0.0.1', 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36 Edg/149.0.0.0', '账号登录成功', '2026-06-15 17:54:50');
INSERT INTO `auth_session_event_log` VALUES (26, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '127.0.0.1', 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-15 17:55:06');
INSERT INTO `auth_session_event_log` VALUES (27, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', 'ec4fd08ee1c272753259cd9088f713bcf46c2b5e4b02541a3b64c8382ccc7731', '127.0.0.1', 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-15 17:55:06');
INSERT INTO `auth_session_event_log` VALUES (28, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-15 18:34:39');
INSERT INTO `auth_session_event_log` VALUES (29, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', 'c3f43e0a731edf449c8ce688dc8c90d49520659d66e6140d83b8b0b8e6035cb2', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '账号登录成功，已启用同端单登录策略', '2026-06-15 18:34:39');
INSERT INTO `auth_session_event_log` VALUES (30, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-15 18:34:39');
INSERT INTO `auth_session_event_log` VALUES (31, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', '7b27af9b2798840e5b04813e0af86e3bda450c256ad892dc7aeac0568055ff3f', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '账号登录成功，已启用同端单登录策略', '2026-06-15 18:34:39');
INSERT INTO `auth_session_event_log` VALUES (32, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-15 18:34:40');
INSERT INTO `auth_session_event_log` VALUES (33, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', '21b9de6485234c213543c1234eb53e6bcc2c958eb94bb3c533ae5e6dfeea3ac1', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '账号登录成功，已启用同端单登录策略', '2026-06-15 18:34:40');
INSERT INTO `auth_session_event_log` VALUES (34, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-15 18:34:40');
INSERT INTO `auth_session_event_log` VALUES (35, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', '4371aaa375ef62b8f0da1071a757db0a85155a94e0b6856bbd9c2eec5978559a', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '账号登录成功，已启用同端单登录策略', '2026-06-15 18:34:40');
INSERT INTO `auth_session_event_log` VALUES (36, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', 'fe0da479d636197a81d428477fdb80bdd8909bf7ffc2eb2b9df180add62f0bc8', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '账号登录成功', '2026-06-15 18:38:21');
INSERT INTO `auth_session_event_log` VALUES (37, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-15 18:38:21');
INSERT INTO `auth_session_event_log` VALUES (38, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', '8f98d7e5ba19428320cfdd1143ea7515d9ec73af9ecd1ba7502d5b274f728a88', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '账号登录成功，已启用同端单登录策略', '2026-06-15 18:38:21');
INSERT INTO `auth_session_event_log` VALUES (39, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', 'c6c75d3bd92b92f726fd38fab6444816a8298a39d5f4c3fcc867a4fdfc26dece', '127.0.0.1', 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36 Edg/149.0.0.0', '账号登录成功', '2026-06-15 19:26:48');
INSERT INTO `auth_session_event_log` VALUES (40, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-15 19:27:09');
INSERT INTO `auth_session_event_log` VALUES (41, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', '2ccfaa37a014f4cb4f8522d6903ed5619f5bc70727a3dab3b37a8f1e206d31f8', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-15 19:27:09');
INSERT INTO `auth_session_event_log` VALUES (42, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', '1f737b40d617851bb1dd17381e99a4676e5171c57c404d9383708760b3a533d9', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '账号登录成功', '2026-06-16 00:48:38');
INSERT INTO `auth_session_event_log` VALUES (43, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-16 00:48:39');
INSERT INTO `auth_session_event_log` VALUES (44, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', '927eba88000f851ff481abd84acb8b1d9f7a8d9c34dc45cd7b98e313b0a010ed', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '账号登录成功，已启用同端单登录策略', '2026-06-16 00:48:39');
INSERT INTO `auth_session_event_log` VALUES (45, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', '236fb4546080e7f7ff13b1b69091fa35b8d58d34bf8d9950c3e6ee24fcb6a252', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '账号登录成功', '2026-06-16 00:55:52');
INSERT INTO `auth_session_event_log` VALUES (46, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-16 00:55:53');
INSERT INTO `auth_session_event_log` VALUES (47, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', '4734b9985def6f77b936d2949afde6d0282b75b4e8e0bd9eda8d1c140efaeceb', '0:0:0:0:0:0:0:1', 'Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8655', '账号登录成功，已启用同端单登录策略', '2026-06-16 00:55:53');
INSERT INTO `auth_session_event_log` VALUES (48, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', 'decc8f3ec8bfdaf38ff8fd1edeecd804d7f83e8894b40ffa65e0aafba7356531', '127.0.0.1', 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36 Edg/149.0.0.0', '账号登录成功', '2026-06-16 01:26:18');
INSERT INTO `auth_session_event_log` VALUES (49, 1, 'USER', '13800000000', 'user-h5', 'LOGOUT', 'SUCCESS', 'USER:1', 'decc8f3ec8bfdaf38ff8fd1edeecd804d7f83e8894b40ffa65e0aafba7356531', '127.0.0.1', 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36 Edg/149.0.0.0', '账号主动退出登录', '2026-06-16 01:26:23');
INSERT INTO `auth_session_event_log` VALUES (50, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', '5ec0ef63e631d9c5c4ef46785bfa4784cd59889c148ec2e222a9839f2a14296b', '127.0.0.1', 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36 Edg/149.0.0.0', '账号登录成功', '2026-06-16 01:26:27');
INSERT INTO `auth_session_event_log` VALUES (51, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-16 02:59:48');
INSERT INTO `auth_session_event_log` VALUES (52, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', '5514f5f3dd763e083570475e1818a411a62d4b0fabeeb78ba5d747b2a305c065', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-16 02:59:48');
INSERT INTO `auth_session_event_log` VALUES (53, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:1', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-16 15:28:48');
INSERT INTO `auth_session_event_log` VALUES (54, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', 'de960a95254b17d4762f0543f8da82325aee3ae014af3250120bb1e88259573d', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-16 15:28:48');
INSERT INTO `auth_session_event_log` VALUES (55, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', '7e06fefbaf419b81cd0d7f97312bd683a1bac87e529a7b60af6f62902fd65072', '127.0.0.1', 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36 Edg/149.0.0.0', '账号登录成功', '2026-06-16 18:24:19');
INSERT INTO `auth_session_event_log` VALUES (56, 4, 'ADMIN', 'stock', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:4', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-16 20:42:50');
INSERT INTO `auth_session_event_log` VALUES (57, 4, 'ADMIN', 'stock', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:4', '3cd693dc8c928f77446f2f7e13338607e77efd983ce6f4f6f3eff09e05efbfa9', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-16 20:42:50');
INSERT INTO `auth_session_event_log` VALUES (58, 4, 'ADMIN', 'stock', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:4', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-16 22:59:15');
INSERT INTO `auth_session_event_log` VALUES (59, 4, 'ADMIN', 'stock', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:4', '44f64ba926d6c7504962ea7719486a7b884e55a772e7038d853afe3ad7e7a582', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-16 22:59:15');
INSERT INTO `auth_session_event_log` VALUES (60, 4, 'ADMIN', 'stock', 'admin-web', 'LOGOUT', 'SUCCESS', 'ADMIN:4', '44f64ba926d6c7504962ea7719486a7b884e55a772e7038d853afe3ad7e7a582', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号主动退出登录', '2026-06-16 22:59:53');
INSERT INTO `auth_session_event_log` VALUES (61, 2, 'ADMIN', 'finance', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:2', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-16 22:59:54');
INSERT INTO `auth_session_event_log` VALUES (62, 2, 'ADMIN', 'finance', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:2', 'df75bf23547bdcaa6cad5f7e3f09150fa189f8fd552b5a5a283e2e85cf75807a', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-16 22:59:54');
INSERT INTO `auth_session_event_log` VALUES (63, 4, 'ADMIN', 'stock', 'admin-web', 'LOGIN_REPLACED', 'SUCCESS', 'ADMIN:4', NULL, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '您的后台账号已在其他浏览器或设备登录，当前页面已退出。若非本人操作，请及时修改密码或联系超级管理员。', '2026-06-17 01:57:58');
INSERT INTO `auth_session_event_log` VALUES (64, 4, 'ADMIN', 'stock', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:4', 'f25451d50be03fe36ef4f50e60c6e8ad461c306dff6a6cdc34ef937d77312caa', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36 Edg/149.0.0.0', '账号登录成功，已启用同端单登录策略', '2026-06-17 01:57:58');

-- ----------------------------
-- Table structure for cart_item
-- ----------------------------
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '购买数量',
  `checked` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否勾选',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_cart_item_user_sku`(`user_id` ASC, `sku_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '购物车项表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cart_item
-- ----------------------------
INSERT INTO `cart_item` VALUES (1, 1, 1, 2, 1, '2026-06-10 10:00:00', '2026-06-16 00:55:53', '2026-06-16 00:55:53');
INSERT INTO `cart_item` VALUES (2, 2, 2, 2, 1, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `cart_item` VALUES (3, 1, 12, 2, 1, '2026-06-10 03:27:18', '2026-06-15 19:53:38', '2026-06-15 19:53:38');
INSERT INTO `cart_item` VALUES (4, 1, 3, 1, 1, '2026-06-15 01:58:18', '2026-06-15 19:53:38', '2026-06-15 19:53:38');

-- ----------------------------
-- Table structure for fms_file_record
-- ----------------------------
DROP TABLE IF EXISTS `fms_file_record`;
CREATE TABLE `fms_file_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `biz_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型',
  `storage_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储类型',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储文件名',
  `original_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `relative_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '相对路径',
  `access_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访问地址',
  `content_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '文件内容类型',
  `file_size` bigint NOT NULL DEFAULT 0 COMMENT '文件大小字节',
  `uploader_id` bigint NOT NULL COMMENT '上传人ID',
  `uploader_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '上传人类型',
  `uploader_account` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '上传人账号',
  `uploader_nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '上传人昵称',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '文件状态',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_fms_file_record_biz_type`(`biz_type` ASC) USING BTREE,
  INDEX `idx_fms_file_record_uploader_id`(`uploader_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 33 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of fms_file_record
-- ----------------------------
INSERT INTO `fms_file_record` VALUES (1, 'USER_AVATAR', 'LOCAL', '709818c7deca46658210d624f750f770.jpg', '2024_Acer_Consumer_Option_02_3840x2400.jpg', '/avatars/20260506/709818c7deca46658210d624f750f770.jpg', '/uploads/avatars/20260506/709818c7deca46658210d624f750f770.jpg', 'image/jpeg', 867844, 1, 'USER', '13800000000', '陈子涵', 'ACTIVE', 0, '2026-05-06 03:00:24', '2026-05-06 03:00:24', NULL);
INSERT INTO `fms_file_record` VALUES (2, 'USER_AVATAR', 'LOCAL', '9af1071afdf94798bd5b0f9d55d1dba6.jpg', '2024_Acer_Consumer_Option_01_3840x2400.jpg', '/avatars/20260506/9af1071afdf94798bd5b0f9d55d1dba6.jpg', '/uploads/avatars/20260506/9af1071afdf94798bd5b0f9d55d1dba6.jpg', 'image/jpeg', 1020170, 1, 'USER', '13800000000', '陈子涵', 'ACTIVE', 0, '2026-05-06 03:17:44', '2026-05-06 03:17:44', NULL);
INSERT INTO `fms_file_record` VALUES (3, 'USER_AVATAR', 'LOCAL', '217630c2f60c4e74819e379ccdc02208.jpg', '2024_Acer_Consumer_Default_3840x2400.jpg', '/avatars/20260506/217630c2f60c4e74819e379ccdc02208.jpg', '/uploads/avatars/20260506/217630c2f60c4e74819e379ccdc02208.jpg', 'image/jpeg', 1071271, 1, 'USER', '13800000000', '陈子涵', 'ACTIVE', 0, '2026-05-06 03:47:41', '2026-05-06 03:47:41', NULL);
INSERT INTO `fms_file_record` VALUES (4, 'USER_AVATAR', 'LOCAL', 'f50947c47412424ea4d8c5b4e67bc536.jpg', '2024_Acer_Consumer_Default_3840x2400.jpg', '/avatars/20260506/f50947c47412424ea4d8c5b4e67bc536.jpg', '/uploads/avatars/20260506/f50947c47412424ea4d8c5b4e67bc536.jpg', 'image/jpeg', 1071271, 1, 'USER', '13800000000', '陈子涵', 'ACTIVE', 0, '2026-05-06 03:47:51', '2026-05-06 03:47:51', NULL);
INSERT INTO `fms_file_record` VALUES (5, 'USER_AVATAR', 'LOCAL', 'f13f1b180fd84c80a01dc240509fbd2e.jpg', '2024_Acer_Consumer_Default_3840x2400.jpg', '/avatars/20260506/f13f1b180fd84c80a01dc240509fbd2e.jpg', '/uploads/avatars/20260506/f13f1b180fd84c80a01dc240509fbd2e.jpg', 'image/jpeg', 1071271, 1, 'USER', '13800000000', '陈子涵', 'ACTIVE', 0, '2026-05-06 04:06:14', '2026-05-06 04:06:14', NULL);
INSERT INTO `fms_file_record` VALUES (6, 'USER_AVATAR', 'LOCAL', 'cf84342e73014468beff0dd52b48a80c.jpg', '2024_Acer_Consumer_Default_3840x2400.jpg', '/avatars/20260506/cf84342e73014468beff0dd52b48a80c.jpg', '/uploads/avatars/20260506/cf84342e73014468beff0dd52b48a80c.jpg', 'image/jpeg', 1071271, 1, 'USER', '13800000000', '陈子涵', 'ACTIVE', 0, '2026-05-06 04:14:29', '2026-05-06 04:14:29', NULL);
INSERT INTO `fms_file_record` VALUES (7, 'USER_AVATAR', 'LOCAL', '9777a647889d42368ec0558da80fea70.jpg', 'pixnio-3840x2892.jpg', '/avatars/20260506/9777a647889d42368ec0558da80fea70.jpg', '/uploads/avatars/20260506/9777a647889d42368ec0558da80fea70.jpg', 'image/jpeg', 1969885, 1, 'USER', '13800000000', '陈子涵', 'ACTIVE', 0, '2026-05-06 04:16:02', '2026-05-06 04:16:02', NULL);
INSERT INTO `fms_file_record` VALUES (8, 'USER_AVATAR', 'local', 'ef41501df96344f9a507916e752645f0.png', '【哲风壁纸】户外-活力少女-清新.png', 'avatar/2026/05/22/ef41501df96344f9a507916e752645f0.png', '/uploads/avatar/2026/05/22/ef41501df96344f9a507916e752645f0.png', 'image/png', 806894, 3, 'USER', '13800000002', '李若溪', 'ACTIVE', 0, '2026-05-22 22:33:35', '2026-05-22 22:33:35', NULL);
INSERT INTO `fms_file_record` VALUES (9, 'USER_AVATAR', 'local', '31e743dd16434ad9b0294d5e91dbc492.png', '【哲风壁纸】SNAFFUR-动漫少女.png', 'avatar/2026/05/22/31e743dd16434ad9b0294d5e91dbc492.png', '/uploads/avatar/2026/05/22/31e743dd16434ad9b0294d5e91dbc492.png', 'image/png', 734373, 2, 'USER', '13800000001', '王明远', 'ACTIVE', 0, '2026-05-22 22:36:57', '2026-05-22 22:36:57', NULL);
INSERT INTO `fms_file_record` VALUES (10, 'PRODUCT_IMAGE', 'local', '5f260d8f3bf148c8ace847a4e010e90e.png', '【哲风壁纸】三丽鸥-休闲-卡通.png', 'product/2026/05/24/5f260d8f3bf148c8ace847a4e010e90e.png', '/uploads/product/2026/05/24/5f260d8f3bf148c8ace847a4e010e90e.png', 'image/png', 357291, 1, 'ADMIN', 'admin', '客服01', 'ACTIVE', 0, '2026-05-24 00:23:16', '2026-05-24 00:23:16', NULL);
INSERT INTO `fms_file_record` VALUES (11, 'PRODUCT_IMAGE', 'local', 'b15c55eb0a85458cb49f16ab8c1e2027.png', '【哲风壁纸】三丽鸥-休闲-卡通.png', 'product/2026/05/24/b15c55eb0a85458cb49f16ab8c1e2027.png', '/uploads/product/2026/05/24/b15c55eb0a85458cb49f16ab8c1e2027.png', 'image/png', 357291, 1, 'ADMIN', 'admin', '客服01', 'ACTIVE', 0, '2026-05-24 09:41:01', '2026-05-24 09:41:01', NULL);
INSERT INTO `fms_file_record` VALUES (12, 'PRODUCT_IMAGE', 'local', 'c1fbcb7746f64a2495c57eaab5ccbc8f.png', '2ba7d01257a5faaa.png', 'product/2026/05/24/c1fbcb7746f64a2495c57eaab5ccbc8f.png', '/uploads/product/2026/05/24/c1fbcb7746f64a2495c57eaab5ccbc8f.png', 'image/png', 26951, 1, 'ADMIN', 'admin', '客服01', 'ACTIVE', 0, '2026-05-24 09:42:37', '2026-05-24 09:42:37', NULL);
INSERT INTO `fms_file_record` VALUES (13, 'PRODUCT_IMAGE', 'local', 'd33fd7b5c9f04de98b7e93527bb6f0fa.png', '1036146158.png', 'product/2026/05/24/d33fd7b5c9f04de98b7e93527bb6f0fa.png', '/uploads/product/2026/05/24/d33fd7b5c9f04de98b7e93527bb6f0fa.png', 'image/png', 1643440, 1, 'ADMIN', 'admin', '客服01', 'ACTIVE', 0, '2026-05-24 09:52:49', '2026-05-24 09:52:49', NULL);
INSERT INTO `fms_file_record` VALUES (14, 'PRODUCT_IMAGE', 'local', '5e5c00ecbd724688b37d86993ca3fdb3.png', '篮球涂鸦插画素材-427px_爱给网_aigei_com.png', 'product/2026/05/24/5e5c00ecbd724688b37d86993ca3fdb3.png', '/uploads/product/2026/05/24/5e5c00ecbd724688b37d86993ca3fdb3.png', 'image/png', 116751, 1, 'ADMIN', 'admin', '客服01', 'ACTIVE', 0, '2026-05-24 09:59:02', '2026-05-24 09:59:02', NULL);
INSERT INTO `fms_file_record` VALUES (15, 'PRODUCT_IMAGE', 'local', 'c2a22ad843da4b1892fdad5922d68aa8.jpg', '鸡蛋.jpg', 'product/2026/06/02/c2a22ad843da4b1892fdad5922d68aa8.jpg', '/uploads/product/2026/06/02/c2a22ad843da4b1892fdad5922d68aa8.jpg', 'image/jpeg', 432728, 1, 'ADMIN', 'admin', '客服01', 'ACTIVE', 0, '2026-06-02 22:44:14', '2026-06-02 22:44:14', NULL);
INSERT INTO `fms_file_record` VALUES (16, 'PRODUCT_IMAGE', 'local', 'd62ab9fa5f8b4301a7e61db005d9fbf9.jpg', '西柚.jpg', 'product/2026/06/02/d62ab9fa5f8b4301a7e61db005d9fbf9.jpg', '/uploads/product/2026/06/02/d62ab9fa5f8b4301a7e61db005d9fbf9.jpg', 'image/jpeg', 524115, 1, 'ADMIN', 'admin', '客服01', 'ACTIVE', 0, '2026-06-02 22:44:25', '2026-06-02 22:44:25', NULL);
INSERT INTO `fms_file_record` VALUES (17, 'PRODUCT_IMAGE', 'local', '51e4c9503cf444d889d70b6350cf46de.png', 'labubu拉布布泡泡玛特 (232)-869px_爱给网_aigei_com.png', 'product/2026/06/02/51e4c9503cf444d889d70b6350cf46de.png', '/uploads/product/2026/06/02/51e4c9503cf444d889d70b6350cf46de.png', 'image/png', 751048, 1, 'ADMIN', 'admin', '客服01', 'ACTIVE', 0, '2026-06-02 22:44:35', '2026-06-02 22:44:35', NULL);
INSERT INTO `fms_file_record` VALUES (18, 'PRODUCT_IMAGE', 'local', '0310591820174808be231d21753441d9.png', '纸巾免扣元素-400px_爱给网_aigei_com.png', 'product/2026/06/02/0310591820174808be231d21753441d9.png', '/uploads/product/2026/06/02/0310591820174808be231d21753441d9.png', 'image/png', 95644, 1, 'ADMIN', 'admin', '客服01', 'ACTIVE', 0, '2026-06-02 22:45:46', '2026-06-02 22:45:46', NULL);
INSERT INTO `fms_file_record` VALUES (19, 'USER_AVATAR', 'local', '3e45bbfedb234afea44177e17b15c104.png', '【哲风壁纸】户外-活力少女-清新.png', 'avatar/2026/06/10/3e45bbfedb234afea44177e17b15c104.png', '/uploads/avatar/2026/06/10/3e45bbfedb234afea44177e17b15c104.png', 'image/png', 806894, 1, 'USER', '13800000000', '对账测试用户', 'ACTIVE', 0, '2026-06-10 02:41:25', '2026-06-10 02:41:25', NULL);
INSERT INTO `fms_file_record` VALUES (20, 'PRODUCT_IMAGE', 'local', '562c08ac753e4bb0a7bbc7d6eabe7d9c.jpg', 'pixnio-5335x3556.jpg', 'product/2026/06/10/562c08ac753e4bb0a7bbc7d6eabe7d9c.jpg', '/uploads/product/2026/06/10/562c08ac753e4bb0a7bbc7d6eabe7d9c.jpg', 'image/jpeg', 1582787, 1, 'ADMIN', 'admin', '超级管理员', 'ACTIVE', 0, '2026-06-10 02:50:41', '2026-06-10 02:50:41', NULL);
INSERT INTO `fms_file_record` VALUES (21, 'PRODUCT_IMAGE', 'local', 'a5ac2983f753438f9ad6cd35239fa671.png', 'image (1).png', 'product/2026/06/10/a5ac2983f753438f9ad6cd35239fa671.png', '/uploads/product/2026/06/10/a5ac2983f753438f9ad6cd35239fa671.png', 'image/png', 779451, 1, 'ADMIN', 'admin', '超级管理员', 'ACTIVE', 0, '2026-06-10 03:24:07', '2026-06-10 03:24:07', NULL);
INSERT INTO `fms_file_record` VALUES (22, 'PRODUCT_IMAGE', 'local', 'd72a34d879a14db3b42abe9e310519bd.jpeg', 'e8cec0b54696c27ef2e300e6c1d16c2c~tplv-be4g95zd3a-image.jpeg', 'product/2026/06/10/d72a34d879a14db3b42abe9e310519bd.jpeg', '/uploads/product/2026/06/10/d72a34d879a14db3b42abe9e310519bd.jpeg', 'image/jpeg', 71461, 1, 'ADMIN', 'admin', '超级管理员', 'ACTIVE', 0, '2026-06-10 03:24:15', '2026-06-10 03:24:15', NULL);
INSERT INTO `fms_file_record` VALUES (23, 'PRODUCT_IMAGE', 'local', '3443574991734406bb2f7bfe60b86a8e.jpeg', '7b9e7380fc1522c404a15574359b0c36~tplv-be4g95zd3a-image.jpeg', 'product/2026/06/10/3443574991734406bb2f7bfe60b86a8e.jpeg', '/uploads/product/2026/06/10/3443574991734406bb2f7bfe60b86a8e.jpeg', 'image/jpeg', 38746, 1, 'ADMIN', 'admin', '超级管理员', 'ACTIVE', 0, '2026-06-10 03:24:21', '2026-06-10 03:24:21', NULL);
INSERT INTO `fms_file_record` VALUES (24, 'PRODUCT_IMAGE', 'local', 'ece29ebb73a043138b45b5660aa721a3.jpeg', '0dee462ddd9035c1d7f5cadd8d382c6e~tplv-be4g95zd3a-image.jpeg', 'product/2026/06/10/ece29ebb73a043138b45b5660aa721a3.jpeg', '/uploads/product/2026/06/10/ece29ebb73a043138b45b5660aa721a3.jpeg', 'image/jpeg', 109591, 1, 'ADMIN', 'admin', '超级管理员', 'ACTIVE', 0, '2026-06-10 03:24:30', '2026-06-10 03:24:30', NULL);
INSERT INTO `fms_file_record` VALUES (25, 'PRODUCT_IMAGE', 'local', '33b22da0a55b4e5ba0bf285ada3f866e.jpeg', '6de5b64fb1e11de26b7a28f1903dc263~tplv-be4g95zd3a-image.jpeg', 'product/2026/06/10/33b22da0a55b4e5ba0bf285ada3f866e.jpeg', '/uploads/product/2026/06/10/33b22da0a55b4e5ba0bf285ada3f866e.jpeg', 'image/jpeg', 59564, 1, 'ADMIN', 'admin', '超级管理员', 'ACTIVE', 0, '2026-06-10 03:24:36', '2026-06-10 03:24:36', NULL);
INSERT INTO `fms_file_record` VALUES (26, 'PRODUCT_IMAGE', 'local', '2741559b83754426bc90663c1c65c04f.jpeg', 'a798f573da730e39b6718850fd6b83bd~tplv-be4g95zd3a-image.jpeg', 'product/2026/06/10/2741559b83754426bc90663c1c65c04f.jpeg', '/uploads/product/2026/06/10/2741559b83754426bc90663c1c65c04f.jpeg', 'image/jpeg', 70452, 1, 'ADMIN', 'admin', '超级管理员', 'ACTIVE', 0, '2026-06-10 03:24:43', '2026-06-10 03:24:43', NULL);
INSERT INTO `fms_file_record` VALUES (27, 'PRODUCT_IMAGE', 'local', '3ea59cfc5e744be58ef88bde491c5d78.jpeg', 'XfHKAEkH_m_34bc5a49124290cc4060bc39c8a3bf70_sx_128266_www800-800~tplv-be4g95zd3a-image.jpeg', 'product/2026/06/10/3ea59cfc5e744be58ef88bde491c5d78.jpeg', '/uploads/product/2026/06/10/3ea59cfc5e744be58ef88bde491c5d78.jpeg', 'image/jpeg', 115153, 1, 'ADMIN', 'admin', '超级管理员', 'ACTIVE', 0, '2026-06-10 03:24:51', '2026-06-10 03:24:51', NULL);
INSERT INTO `fms_file_record` VALUES (28, 'PRODUCT_IMAGE', 'local', '59908e0eff0c4e7888e7145c44c4cf28.jpeg', '3f6fca65076a3d96a2700afc8830e7d9~tplv-be4g95zd3a-image.jpeg', 'product/2026/06/10/59908e0eff0c4e7888e7145c44c4cf28.jpeg', '/uploads/product/2026/06/10/59908e0eff0c4e7888e7145c44c4cf28.jpeg', 'image/jpeg', 153579, 1, 'ADMIN', 'admin', '超级管理员', 'ACTIVE', 0, '2026-06-10 03:25:04', '2026-06-10 03:25:04', NULL);
INSERT INTO `fms_file_record` VALUES (29, 'PRODUCT_IMAGE', 'local', '48e6e507f29f475790e6c1a459270605.png', '护眼台灯-374px_爱给网_aigei_com.png', 'product/2026/06/10/48e6e507f29f475790e6c1a459270605.png', '/uploads/product/2026/06/10/48e6e507f29f475790e6c1a459270605.png', 'image/png', 60857, 1, 'ADMIN', 'admin', '超级管理员', 'ACTIVE', 0, '2026-06-10 03:25:11', '2026-06-10 03:25:11', NULL);
INSERT INTO `fms_file_record` VALUES (30, 'PRODUCT_IMAGE', 'local', 'a83bd60d6f1f4bba818788d9f99210f6.png', 'image.png', 'product/2026/06/10/a83bd60d6f1f4bba818788d9f99210f6.png', '/uploads/product/2026/06/10/a83bd60d6f1f4bba818788d9f99210f6.png', 'image/png', 964404, 1, 'ADMIN', 'admin', '超级管理员', 'ACTIVE', 0, '2026-06-10 03:25:18', '2026-06-10 03:25:18', NULL);
INSERT INTO `fms_file_record` VALUES (31, 'PRODUCT_IMAGE', 'local', '3b8baa11c6d949dd9b4cf84611438534.png', '1036146158.png', 'product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 'image/png', 1643440, 1, 'ADMIN', 'admin', '超级管理员', 'ACTIVE', 0, '2026-06-10 03:25:24', '2026-06-10 03:25:24', NULL);
INSERT INTO `fms_file_record` VALUES (32, 'PRODUCT_IMAGE', 'local', 'd2594ea8625e44a986eba8f9476e91e5.png', 'labubu拉布布泡泡玛特 (232)-869px_爱给网_aigei_com.png', 'product/2026/06/16/d2594ea8625e44a986eba8f9476e91e5.png', '/uploads/product/2026/06/16/d2594ea8625e44a986eba8f9476e91e5.png', 'image/png', 751048, 1, 'ADMIN', 'admin', 'P2 Admin Smoke', 'ACTIVE', 0, '2026-06-16 20:56:27', '2026-06-16 20:56:27', NULL);

-- ----------------------------
-- Table structure for ims_stock
-- ----------------------------
DROP TABLE IF EXISTS `ims_stock`;
CREATE TABLE `ims_stock`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '库存主键',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `total_stock` int NOT NULL DEFAULT 0 COMMENT '总库存',
  `locked_stock` int NOT NULL DEFAULT 0 COMMENT '已锁库存',
  `available_stock` int NOT NULL DEFAULT 0 COMMENT '可售库存',
  `stock_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '库存状态',
  `low_stock_threshold` int NOT NULL DEFAULT 10 COMMENT '低库存预警阈值',
  `high_stock_threshold` int NOT NULL DEFAULT 1000 COMMENT '高库存预警阈值',
  `warning_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NORMAL' COMMENT '库存预警状态',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ims_stock_sku_id`(`sku_id` ASC) USING BTREE,
  INDEX `idx_ims_stock_status_warning_sku`(`stock_status` ASC, `warning_status` ASC, `sku_id` ASC) USING BTREE,
  INDEX `idx_ims_stock_warning_status_sku`(`warning_status` ASC, `sku_id` ASC) USING BTREE,
  INDEX `idx_ims_stock_stock_status_sku`(`stock_status` ASC, `sku_id` ASC) USING BTREE,
  CONSTRAINT `chk_ims_stock_quantity_relation` CHECK (`total_stock` = (`locked_stock` + `available_stock`))
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ims_stock
-- ----------------------------
INSERT INTO `ims_stock` VALUES (1, 1, 110, 0, 110, 'ACTIVE', 1, 9999, 'NORMAL', 45, '2026-01-01 09:00:00', '2026-06-16 00:55:54');
INSERT INTO `ims_stock` VALUES (2, 2, 122, 0, 122, 'ACTIVE', 10, 1000, 'NORMAL', 1, '2026-01-01 09:00:00', '2026-06-10 18:15:33');
INSERT INTO `ims_stock` VALUES (3, 3, 122, 0, 122, 'ACTIVE', 10, 1000, 'NORMAL', 2, '2026-01-01 09:00:00', '2026-06-15 19:53:43');
INSERT INTO `ims_stock` VALUES (4, 4, 124, 0, 124, 'ACTIVE', 10, 1000, 'NORMAL', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00');
INSERT INTO `ims_stock` VALUES (5, 5, 125, 0, 125, 'ACTIVE', 10, 1000, 'NORMAL', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00');
INSERT INTO `ims_stock` VALUES (6, 6, 126, 0, 126, 'ACTIVE', 10, 1000, 'NORMAL', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00');
INSERT INTO `ims_stock` VALUES (7, 7, 127, 0, 127, 'ACTIVE', 10, 1000, 'NORMAL', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00');
INSERT INTO `ims_stock` VALUES (8, 8, 136, 0, 136, 'ACTIVE', 10, 1000, 'NORMAL', 3, '2026-01-01 09:00:00', '2026-06-10 15:27:20');
INSERT INTO `ims_stock` VALUES (9, 9, 130, 0, 130, 'ACTIVE', 10, 1000, 'NORMAL', 1, '2026-01-01 09:00:00', '2026-06-10 15:54:47');
INSERT INTO `ims_stock` VALUES (10, 10, 130, 0, 130, 'ACTIVE', 10, 1000, 'NORMAL', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00');
INSERT INTO `ims_stock` VALUES (11, 11, 128, 0, 128, 'ACTIVE', 10, 1000, 'NORMAL', 6, '2026-01-01 09:00:00', '2026-06-12 01:58:14');
INSERT INTO `ims_stock` VALUES (12, 12, 130, 0, 130, 'ACTIVE', 10, 1000, 'NORMAL', 7, '2026-01-01 09:00:00', '2026-06-15 19:53:43');
INSERT INTO `ims_stock` VALUES (13, 13, 200, 0, 200, 'ACTIVE', 1, 1000, 'NORMAL', 1, '2026-06-19 14:16:53', '2026-06-19 14:20:12');
INSERT INTO `ims_stock` VALUES (14, 14, 200, 0, 200, 'ACTIVE', 10, 1000, 'NORMAL', 0, '2026-06-19 14:18:50', '2026-06-19 14:20:14');

-- ----------------------------
-- Table structure for ims_stock_lock
-- ----------------------------
DROP TABLE IF EXISTS `ims_stock_lock`;
CREATE TABLE `ims_stock_lock`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '库存锁记录ID',
  `lock_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '库存锁单号',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `business_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务类型',
  `business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务单号',
  `quantity` int NOT NULL COMMENT '锁定数量',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '锁状态',
  `lock_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '锁定时间',
  `release_time` datetime NULL DEFAULT NULL COMMENT '释放时间',
  `deduct_time` datetime NULL DEFAULT NULL COMMENT '扣减时间',
  `reserved_synced` tinyint(1) NOT NULL DEFAULT 0 COMMENT '预占库存是否已同步DB库存表',
  `cancelled_synced` tinyint(1) NOT NULL DEFAULT 0 COMMENT '取消预占是否已同步DB库存表',
  `confirmed_synced` tinyint(1) NOT NULL DEFAULT 0 COMMENT '确认扣减是否已同步DB库存表',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ims_stock_lock_no`(`lock_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_ims_stock_lock_business`(`business_type` ASC, `business_no` ASC, `sku_id` ASC) USING BTREE,
  INDEX `idx_ims_stock_lock_sync_status`(`status` ASC, `reserved_synced` ASC, `lock_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存锁记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ims_stock_lock
-- ----------------------------
INSERT INTO `ims_stock_lock` VALUES (1, 'LOCK-RCT202606100009-001', 2, 'ORDER', 'RCT202606100009', 1, 'LOCKED', '2026-06-10 10:00:00', NULL, NULL, 1, 0, 0, '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `ims_stock_lock` VALUES (2, '1fb46d52da3e442a958b657afdaa4007', 12, 'ORDER', 'ORD17810335613035A1625', 1, 'CANCELLED', '2026-06-10 03:32:41', '2026-06-10 03:47:41', NULL, 1, 1, 0, '2026-06-10 03:32:41', '2026-06-10 03:47:41');
INSERT INTO `ims_stock_lock` VALUES (3, '4904cc055b214428a6e62ac57dacd8f8', 11, 'ORDER', 'ORD1781075976142E01526', 1, 'CONFIRMED', '2026-06-10 15:19:36', NULL, '2026-06-10 15:19:40', 1, 0, 1, '2026-06-10 15:19:35', '2026-06-10 15:19:40');
INSERT INTO `ims_stock_lock` VALUES (4, '50b0cb973a65465b8d92ea19940a9486', 11, 'ORDER', 'ORD1781076001361016574', 1, 'CONFIRMED', '2026-06-10 15:20:01', NULL, '2026-06-10 15:20:06', 1, 0, 1, '2026-06-10 15:20:01', '2026-06-10 15:20:05');
INSERT INTO `ims_stock_lock` VALUES (5, 'fb9db462b8ac49aebc1b7f405f36aec2', 8, 'ORDER', 'ORD17810762227909DD3A7', 1, 'CANCELLED', '2026-06-10 15:23:43', '2026-06-10 15:27:21', NULL, 1, 1, 0, '2026-06-10 15:23:42', '2026-06-10 15:27:20');
INSERT INTO `ims_stock_lock` VALUES (6, 'a4594d8010d34931b2d819200f997dea', 12, 'ORDER', 'ORD17810814566188EA786', 1, 'CANCELLED', '2026-06-10 16:50:57', '2026-06-10 16:51:49', NULL, 1, 1, 0, '2026-06-10 16:50:56', '2026-06-10 16:51:48');
INSERT INTO `ims_stock_lock` VALUES (7, '6d5c10ff8197461bb9906121639574e6', 11, 'ORDER', 'ORD17812006903496B061E', 1, 'CONFIRMED', '2026-06-12 01:58:10', NULL, '2026-06-12 01:58:14', 1, 0, 1, '2026-06-12 01:58:10', '2026-06-12 01:58:14');
INSERT INTO `ims_stock_lock` VALUES (8, '3ba12d2ef532483e825799229cef4e91', 1, 'ORDER', 'ORD178151990140736FC43', 1, 'CONFIRMED', '2026-06-15 18:38:22', NULL, '2026-06-15 18:38:22', 1, 0, 1, '2026-06-15 18:38:21', '2026-06-15 18:38:21');
INSERT INTO `ims_stock_lock` VALUES (9, '308cdc98bd3742b1908cbdf8ab521271', 1, 'ORDER', 'ORD17815228604692C7570', 1, 'CONFIRMED', '2026-06-15 19:27:41', NULL, '2026-06-15 19:27:41', 1, 0, 1, '2026-06-15 19:27:40', '2026-06-15 19:27:40');
INSERT INTO `ims_stock_lock` VALUES (10, 'd4b94f73bd3c41788ab11b25a11711c7', 1, 'ORDER', 'AUTO-STOCK-20260615192742', 1, 'CANCELLED', '2026-06-15 19:27:43', '2026-06-15 19:27:43', NULL, 1, 1, 0, '2026-06-15 19:27:42', '2026-06-15 19:27:42');
INSERT INTO `ims_stock_lock` VALUES (11, 'e52023619e544b4aac1b82d00584d6d2', 1, 'ORDER', 'ORD178152381362132B276', 1, 'CONFIRMED', '2026-06-15 19:43:34', NULL, '2026-06-15 19:43:34', 1, 0, 1, '2026-06-15 19:43:33', '2026-06-15 19:43:33');
INSERT INTO `ims_stock_lock` VALUES (12, '90b55e592b1340d0abe7d3969b2d5291', 1, 'ORDER', 'ORD17815238604099B7C62', 1, 'CONFIRMED', '2026-06-15 19:44:20', NULL, '2026-06-15 19:44:21', 1, 0, 1, '2026-06-15 19:44:20', '2026-06-15 19:44:20');
INSERT INTO `ims_stock_lock` VALUES (13, 'e6a348b3c3724988998ec545378718f3', 1, 'ORDER', 'ORD1781523940797C6EC98', 1, 'CONFIRMED', '2026-06-15 19:45:41', NULL, '2026-06-15 19:45:41', 1, 0, 1, '2026-06-15 19:45:40', '2026-06-15 19:45:40');
INSERT INTO `ims_stock_lock` VALUES (14, '4403ad1ffa2249e5992d3dd75247c3dc', 1, 'ORDER', 'ORD1781524329822DF7ACD', 1, 'CONFIRMED', '2026-06-15 19:52:10', NULL, '2026-06-15 19:52:10', 1, 0, 1, '2026-06-15 19:52:09', '2026-06-15 19:52:10');
INSERT INTO `ims_stock_lock` VALUES (15, '35d09c940f824f0c90a889a8e23f3451', 1, 'ORDER', 'ORD178152439679881F09C', 1, 'CONFIRMED', '2026-06-15 19:53:17', NULL, '2026-06-15 19:53:17', 1, 0, 1, '2026-06-15 19:53:16', '2026-06-15 19:53:16');
INSERT INTO `ims_stock_lock` VALUES (16, 'e7b0a8d6ef424b81a1ffea99bcafdd05', 3, 'ORDER', 'ORD1781524417974BA4CFF', 1, 'CONFIRMED', '2026-06-15 19:53:38', NULL, '2026-06-15 19:53:44', 1, 0, 1, '2026-06-15 19:53:38', '2026-06-15 19:53:43');
INSERT INTO `ims_stock_lock` VALUES (17, 'a704bfc2ca284b2295cc76cb47e2b11b', 12, 'ORDER', 'ORD1781524417974BA4CFF', 2, 'CONFIRMED', '2026-06-15 19:53:38', NULL, '2026-06-15 19:53:44', 1, 0, 1, '2026-06-15 19:53:38', '2026-06-15 19:53:43');
INSERT INTO `ims_stock_lock` VALUES (18, 'a729df0539ed42a595ede5cf5551dc8a', 1, 'ORDER', 'ORD1781526648573A839F8', 1, 'CONFIRMED', '2026-06-15 20:30:49', NULL, '2026-06-15 20:30:49', 1, 0, 1, '2026-06-15 20:30:48', '2026-06-15 20:30:48');
INSERT INTO `ims_stock_lock` VALUES (19, '99c90bab6fa149189985aacb11b1726e', 1, 'ORDER', 'ORD178152672263850B15C', 1, 'CONFIRMED', '2026-06-15 20:32:03', NULL, '2026-06-15 20:32:03', 1, 0, 1, '2026-06-15 20:32:02', '2026-06-15 20:32:02');
INSERT INTO `ims_stock_lock` VALUES (20, 'f861a56fc98546e1a75d7f5d0d851eb0', 1, 'ORDER', 'ORD178152809766513D1B9', 1, 'CONFIRMED', '2026-06-15 20:54:58', NULL, '2026-06-15 20:54:58', 1, 0, 1, '2026-06-15 20:54:57', '2026-06-15 20:54:57');
INSERT INTO `ims_stock_lock` VALUES (21, 'ce60aa92308f40bf9ead6a7e037acaae', 1, 'ORDER', 'ORD1781528713367F731BD', 1, 'CONFIRMED', '2026-06-15 21:05:13', NULL, '2026-06-15 21:05:14', 1, 0, 1, '2026-06-15 21:05:13', '2026-06-15 21:05:13');
INSERT INTO `ims_stock_lock` VALUES (22, '6377b48d0cdf4edc8046ca578f8e9219', 1, 'ORDER', 'P5-STOCK-20260615210514', 1, 'CANCELLED', '2026-06-15 21:05:14', '2026-06-15 21:05:14', NULL, 1, 1, 0, '2026-06-15 21:05:13', '2026-06-15 21:05:14');
INSERT INTO `ims_stock_lock` VALUES (23, '20bfe2d8853e4d02898cc279f11ed5ef', 1, 'ORDER', 'P6-STOCK-20260615211510', 1, 'CANCELLED', '2026-06-15 21:15:11', '2026-06-15 21:15:11', NULL, 1, 1, 0, '2026-06-15 21:15:11', '2026-06-15 21:15:11');
INSERT INTO `ims_stock_lock` VALUES (24, '8ffb4e717308491990d966702bd64fee', 1, 'ORDER', 'ORD17815421181506A2B36', 1, 'CONFIRMED', '2026-06-16 00:48:38', NULL, '2026-06-16 00:48:38', 1, 0, 1, '2026-06-16 00:48:38', '2026-06-16 00:48:38');
INSERT INTO `ims_stock_lock` VALUES (25, '7827159b09ca4084a6d9797ebacb5bf2', 1, 'ORDER', 'AUTO-STOCK-20260616004840', 1, 'CANCELLED', '2026-06-16 00:48:40', '2026-06-16 00:48:40', NULL, 1, 1, 0, '2026-06-16 00:48:40', '2026-06-16 00:48:40');
INSERT INTO `ims_stock_lock` VALUES (26, 'bcd649a9057348bb80e6a36190505de7', 1, 'ORDER', 'ORD17815425521398BF4BA', 1, 'CONFIRMED', '2026-06-16 00:55:52', NULL, '2026-06-16 00:55:52', 1, 0, 1, '2026-06-16 00:55:52', '2026-06-16 00:55:52');
INSERT INTO `ims_stock_lock` VALUES (27, '70443e58c8e648df89f9f219fb20afd1', 1, 'ORDER', 'AUTO-STOCK-20260616005554', 1, 'CANCELLED', '2026-06-16 00:55:55', '2026-06-16 00:55:55', NULL, 1, 1, 0, '2026-06-16 00:55:54', '2026-06-16 00:55:54');

-- ----------------------------
-- Table structure for ims_stock_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `ims_stock_operation_log`;
CREATE TABLE `ims_stock_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '库存操作日志ID',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `operation_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `business_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '业务类型',
  `business_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '业务单号',
  `change_quantity` int NOT NULL DEFAULT 0 COMMENT '变更数量',
  `before_total_stock` int NOT NULL DEFAULT 0 COMMENT '变更前总库存',
  `before_locked_stock` int NOT NULL DEFAULT 0 COMMENT '变更前锁定库存',
  `before_available_stock` int NOT NULL DEFAULT 0 COMMENT '变更前可用库存',
  `after_total_stock` int NOT NULL DEFAULT 0 COMMENT '变更后总库存',
  `after_locked_stock` int NOT NULL DEFAULT 0 COMMENT '变更后锁定库存',
  `after_available_stock` int NOT NULL DEFAULT 0 COMMENT '变更后可用库存',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '备注',
  `operator_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'æ“ä½œäººç±»åž‹',
  `operator_id` bigint NULL DEFAULT NULL COMMENT 'æ“ä½œäººID',
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'æ“ä½œäººåç§°',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'æ¥æºç±»åž‹',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ims_stock_operation_log_biz`(`business_type` ASC, `business_no` ASC, `sku_id` ASC, `operation_type` ASC) USING BTREE,
  INDEX `idx_ims_stock_operation_log_sku_id`(`sku_id` ASC) USING BTREE,
  INDEX `idx_ims_stock_operation_log_operation_type`(`operation_type` ASC) USING BTREE,
  INDEX `idx_ims_stock_operation_log_created_at`(`created_at` ASC) USING BTREE,
  INDEX `idx_ims_stock_operation_log_source_type`(`source_type` ASC) USING BTREE,
  INDEX `idx_ims_stock_operation_log_operator_id`(`operator_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 90 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存操作日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ims_stock_operation_log
-- ----------------------------
INSERT INTO `ims_stock_operation_log` VALUES (1, 1, 'INIT', 'SYSTEM', 'INIT_SKU_1', 121, 0, 0, 0, 121, 0, 121, '初始化库存日志', 'SYSTEM', NULL, 'system', 'INIT', '2026-06-10 10:00:00');
INSERT INTO `ims_stock_operation_log` VALUES (2, 2, 'RECONCILE_DIFF', 'SYSTEM', 'STOCK_DIFF_SKU_2', 0, 122, 0, 122, 122, 0, 122, '库存对账测试差异日志', 'SYSTEM', NULL, 'system', 'RECONCILIATION_JOB', '2026-06-10 10:00:00');
INSERT INTO `ims_stock_operation_log` VALUES (3, 12, 'RESERVE', 'ORDER', 'ORD17810335613035A1625', 1, 132, 0, 132, 132, 1, 131, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-10 03:32:41');
INSERT INTO `ims_stock_operation_log` VALUES (4, 12, 'CANCEL_RESERVE', 'ORDER', 'ORD17810335613035A1625', 1, 132, 1, 131, 132, 0, 132, '释放预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-10 03:47:41');
INSERT INTO `ims_stock_operation_log` VALUES (5, 12, 'CANCEL', 'ORDER', 'ORD17810335613035A1625', 1, 132, 1, 131, 132, 0, 132, 'MQ取消预占落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-10 03:47:41');
INSERT INTO `ims_stock_operation_log` VALUES (6, 8, 'RESTORE', 'ORDER_REFUND', 'RFDRCT202606100008', 8, 128, 0, 128, 136, 0, 136, '回补库存', 'SYSTEM', NULL, 'system', 'ORDER_REFUND', '2026-06-10 15:18:18');
INSERT INTO `ims_stock_operation_log` VALUES (7, 11, 'RESERVE', 'ORDER', 'ORD1781075976142E01526', 1, 131, 0, 131, 131, 1, 130, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-10 15:19:36');
INSERT INTO `ims_stock_operation_log` VALUES (8, 11, 'CONFIRM_DEDUCT', 'ORDER', 'ORD1781075976142E01526', -1, 131, 1, 130, 130, 0, 130, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-10 15:19:40');
INSERT INTO `ims_stock_operation_log` VALUES (9, 11, 'CONFIRM', 'ORDER', 'ORD1781075976142E01526', -1, 131, 1, 130, 130, 0, 130, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-10 15:19:40');
INSERT INTO `ims_stock_operation_log` VALUES (10, 11, 'RESERVE', 'ORDER', 'ORD1781076001361016574', 1, 130, 0, 130, 130, 1, 129, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-10 15:20:01');
INSERT INTO `ims_stock_operation_log` VALUES (11, 11, 'CONFIRM_DEDUCT', 'ORDER', 'ORD1781076001361016574', -1, 130, 1, 129, 129, 0, 129, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-10 15:20:06');
INSERT INTO `ims_stock_operation_log` VALUES (12, 11, 'CONFIRM', 'ORDER', 'ORD1781076001361016574', -1, 130, 1, 129, 129, 0, 129, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-10 15:20:06');
INSERT INTO `ims_stock_operation_log` VALUES (13, 8, 'RESERVE', 'ORDER', 'ORD17810762227909DD3A7', 1, 136, 0, 136, 136, 1, 135, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-10 15:23:43');
INSERT INTO `ims_stock_operation_log` VALUES (14, 8, 'CANCEL_RESERVE', 'ORDER', 'ORD17810762227909DD3A7', 1, 136, 1, 135, 136, 0, 136, '释放预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-10 15:27:21');
INSERT INTO `ims_stock_operation_log` VALUES (15, 8, 'CANCEL', 'ORDER', 'ORD17810762227909DD3A7', 1, 136, 1, 135, 136, 0, 136, 'MQ取消预占落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-10 15:27:21');
INSERT INTO `ims_stock_operation_log` VALUES (16, 9, 'RESTORE', 'ORDER_REFUND', 'ORF1781078073650ED3D88', 1, 129, 0, 129, 130, 0, 130, '回补库存', 'SYSTEM', NULL, 'system', 'ORDER_REFUND', '2026-06-10 15:54:47');
INSERT INTO `ims_stock_operation_log` VALUES (17, 12, 'RESERVE', 'ORDER', 'ORD17810814566188EA786', 1, 132, 0, 132, 132, 1, 131, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-10 16:50:57');
INSERT INTO `ims_stock_operation_log` VALUES (18, 12, 'CANCEL_RESERVE', 'ORDER', 'ORD17810814566188EA786', 1, 132, 1, 131, 132, 0, 132, '释放预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-10 16:51:49');
INSERT INTO `ims_stock_operation_log` VALUES (19, 12, 'CANCEL', 'ORDER', 'ORD17810814566188EA786', 1, 132, 1, 131, 132, 0, 132, 'MQ取消预占落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-10 16:51:49');
INSERT INTO `ims_stock_operation_log` VALUES (20, 11, 'RESERVE', 'ORDER', 'ORD17812006903496B061E', 1, 129, 0, 129, 129, 1, 128, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-12 01:58:10');
INSERT INTO `ims_stock_operation_log` VALUES (21, 11, 'CONFIRM_DEDUCT', 'ORDER', 'ORD17812006903496B061E', -1, 129, 1, 128, 128, 0, 128, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-12 01:58:14');
INSERT INTO `ims_stock_operation_log` VALUES (22, 11, 'CONFIRM', 'ORDER', 'ORD17812006903496B061E', -1, 129, 1, 128, 128, 0, 128, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-12 01:58:14');
INSERT INTO `ims_stock_operation_log` VALUES (23, 1, 'RESERVE', 'ORDER', 'ORD178151990140736FC43', 1, 121, 0, 121, 121, 1, 120, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 18:38:22');
INSERT INTO `ims_stock_operation_log` VALUES (24, 1, 'CONFIRM_DEDUCT', 'ORDER', 'ORD178151990140736FC43', -1, 121, 1, 120, 120, 0, 120, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 18:38:22');
INSERT INTO `ims_stock_operation_log` VALUES (25, 1, 'CONFIRM', 'ORDER', 'ORD178151990140736FC43', -1, 121, 1, 120, 120, 0, 120, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 18:38:22');
INSERT INTO `ims_stock_operation_log` VALUES (26, 1, 'RESERVE', 'ORDER', 'ORD17815228604692C7570', 1, 120, 0, 120, 120, 1, 119, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:27:41');
INSERT INTO `ims_stock_operation_log` VALUES (27, 1, 'CONFIRM_DEDUCT', 'ORDER', 'ORD17815228604692C7570', -1, 120, 1, 119, 119, 0, 119, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:27:41');
INSERT INTO `ims_stock_operation_log` VALUES (28, 1, 'CONFIRM', 'ORDER', 'ORD17815228604692C7570', -1, 120, 1, 119, 119, 0, 119, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 19:27:41');
INSERT INTO `ims_stock_operation_log` VALUES (29, 1, 'RESERVE', 'ORDER', 'AUTO-STOCK-20260615192742', 1, 119, 0, 119, 119, 1, 118, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:27:43');
INSERT INTO `ims_stock_operation_log` VALUES (30, 1, 'CANCEL_RESERVE', 'ORDER', 'AUTO-STOCK-20260615192742', 1, 119, 1, 118, 119, 0, 119, '释放预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:27:43');
INSERT INTO `ims_stock_operation_log` VALUES (31, 1, 'CANCEL', 'ORDER', 'AUTO-STOCK-20260615192742', 1, 119, 1, 118, 119, 0, 119, 'MQ取消预占落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 19:27:43');
INSERT INTO `ims_stock_operation_log` VALUES (32, 1, 'RESERVE', 'ORDER', 'ORD178152381362132B276', 1, 119, 0, 119, 119, 1, 118, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:43:34');
INSERT INTO `ims_stock_operation_log` VALUES (33, 1, 'CONFIRM_DEDUCT', 'ORDER', 'ORD178152381362132B276', -1, 119, 1, 118, 118, 0, 118, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:43:34');
INSERT INTO `ims_stock_operation_log` VALUES (34, 1, 'CONFIRM', 'ORDER', 'ORD178152381362132B276', -1, 119, 1, 118, 118, 0, 118, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 19:43:34');
INSERT INTO `ims_stock_operation_log` VALUES (35, 1, 'POLICY_UPDATE', 'ADMIN', '1', 0, 118, 0, 118, 118, 0, 118, 'P2 smoke policy check', 'ADMIN', NULL, 'admin', 'ADMIN_UI', '2026-06-15 19:43:34');
INSERT INTO `ims_stock_operation_log` VALUES (36, 1, 'RESERVE', 'ORDER', 'ORD17815238604099B7C62', 1, 118, 0, 118, 118, 1, 117, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:44:20');
INSERT INTO `ims_stock_operation_log` VALUES (37, 1, 'CONFIRM_DEDUCT', 'ORDER', 'ORD17815238604099B7C62', -1, 118, 1, 117, 117, 0, 117, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:44:21');
INSERT INTO `ims_stock_operation_log` VALUES (38, 1, 'CONFIRM', 'ORDER', 'ORD17815238604099B7C62', -1, 118, 1, 117, 117, 0, 117, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 19:44:21');
INSERT INTO `ims_stock_operation_log` VALUES (39, 1, 'RESERVE', 'ORDER', 'ORD1781523940797C6EC98', 1, 117, 0, 117, 117, 1, 116, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:45:41');
INSERT INTO `ims_stock_operation_log` VALUES (40, 1, 'CONFIRM_DEDUCT', 'ORDER', 'ORD1781523940797C6EC98', -1, 117, 1, 116, 116, 0, 116, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:45:41');
INSERT INTO `ims_stock_operation_log` VALUES (41, 1, 'CONFIRM', 'ORDER', 'ORD1781523940797C6EC98', -1, 117, 1, 116, 116, 0, 116, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 19:45:41');
INSERT INTO `ims_stock_operation_log` VALUES (42, 1, 'RESERVE', 'ORDER', 'ORD1781524329822DF7ACD', 1, 116, 0, 116, 116, 1, 115, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:52:10');
INSERT INTO `ims_stock_operation_log` VALUES (43, 1, 'CONFIRM_DEDUCT', 'ORDER', 'ORD1781524329822DF7ACD', -1, 116, 1, 115, 115, 0, 115, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:52:10');
INSERT INTO `ims_stock_operation_log` VALUES (44, 1, 'CONFIRM', 'ORDER', 'ORD1781524329822DF7ACD', -1, 116, 1, 115, 115, 0, 115, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 19:52:10');
INSERT INTO `ims_stock_operation_log` VALUES (45, 1, 'RESERVE', 'ORDER', 'ORD178152439679881F09C', 1, 115, 0, 115, 115, 1, 114, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:53:17');
INSERT INTO `ims_stock_operation_log` VALUES (46, 1, 'CONFIRM_DEDUCT', 'ORDER', 'ORD178152439679881F09C', -1, 115, 1, 114, 114, 0, 114, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:53:17');
INSERT INTO `ims_stock_operation_log` VALUES (47, 1, 'CONFIRM', 'ORDER', 'ORD178152439679881F09C', -1, 115, 1, 114, 114, 0, 114, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 19:53:17');
INSERT INTO `ims_stock_operation_log` VALUES (48, 1, 'MANUAL_ADJUST', 'ADMIN', 'MANUAL_ADJUST_83d1bb23b0e14c95bf2c962cea1fde8e', 1, 114, 0, 114, 115, 0, 115, '补货入库 - 采购入库：auto smoke', 'ADMIN', NULL, 'admin', 'ADMIN_UI', '2026-06-15 19:53:17');
INSERT INTO `ims_stock_operation_log` VALUES (49, 3, 'RESERVE', 'ORDER', 'ORD1781524417974BA4CFF', 1, 123, 0, 123, 123, 1, 122, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:53:38');
INSERT INTO `ims_stock_operation_log` VALUES (50, 12, 'RESERVE', 'ORDER', 'ORD1781524417974BA4CFF', 2, 132, 0, 132, 132, 2, 130, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:53:38');
INSERT INTO `ims_stock_operation_log` VALUES (51, 3, 'CONFIRM_DEDUCT', 'ORDER', 'ORD1781524417974BA4CFF', -1, 123, 1, 122, 122, 0, 122, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:53:44');
INSERT INTO `ims_stock_operation_log` VALUES (52, 3, 'CONFIRM', 'ORDER', 'ORD1781524417974BA4CFF', -1, 123, 1, 122, 122, 0, 122, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 19:53:44');
INSERT INTO `ims_stock_operation_log` VALUES (53, 12, 'CONFIRM_DEDUCT', 'ORDER', 'ORD1781524417974BA4CFF', -2, 132, 2, 130, 130, 0, 130, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 19:53:44');
INSERT INTO `ims_stock_operation_log` VALUES (54, 12, 'CONFIRM', 'ORDER', 'ORD1781524417974BA4CFF', -2, 132, 2, 130, 130, 0, 130, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 19:53:44');
INSERT INTO `ims_stock_operation_log` VALUES (55, 1, 'RESERVE', 'ORDER', 'ORD1781526648573A839F8', 1, 115, 0, 115, 115, 1, 114, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 20:30:49');
INSERT INTO `ims_stock_operation_log` VALUES (56, 1, 'CONFIRM_DEDUCT', 'ORDER', 'ORD1781526648573A839F8', -1, 115, 1, 114, 114, 0, 114, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 20:30:49');
INSERT INTO `ims_stock_operation_log` VALUES (57, 1, 'CONFIRM', 'ORDER', 'ORD1781526648573A839F8', -1, 115, 1, 114, 114, 0, 114, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 20:30:49');
INSERT INTO `ims_stock_operation_log` VALUES (58, 1, 'RESERVE', 'ORDER', 'ORD178152672263850B15C', 1, 114, 0, 114, 114, 1, 113, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 20:32:03');
INSERT INTO `ims_stock_operation_log` VALUES (59, 1, 'CONFIRM_DEDUCT', 'ORDER', 'ORD178152672263850B15C', -1, 114, 1, 113, 113, 0, 113, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 20:32:03');
INSERT INTO `ims_stock_operation_log` VALUES (60, 1, 'CONFIRM', 'ORDER', 'ORD178152672263850B15C', -1, 114, 1, 113, 113, 0, 113, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 20:32:03');
INSERT INTO `ims_stock_operation_log` VALUES (61, 1, 'RESERVE', 'ORDER', 'ORD178152809766513D1B9', 1, 113, 0, 113, 113, 1, 112, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 20:54:58');
INSERT INTO `ims_stock_operation_log` VALUES (62, 1, 'CONFIRM_DEDUCT', 'ORDER', 'ORD178152809766513D1B9', -1, 113, 1, 112, 112, 0, 112, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 20:54:58');
INSERT INTO `ims_stock_operation_log` VALUES (63, 1, 'CONFIRM', 'ORDER', 'ORD178152809766513D1B9', -1, 113, 1, 112, 112, 0, 112, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 20:54:58');
INSERT INTO `ims_stock_operation_log` VALUES (64, 13, 'INIT', 'PRODUCT', '13', 3, 0, 0, 0, 3, 0, 3, '初始化库存', 'SYSTEM', NULL, 'system', 'PRODUCT_EVENT', '2026-06-15 20:54:58');
INSERT INTO `ims_stock_operation_log` VALUES (65, 1, 'RESERVE', 'ORDER', 'ORD1781528713367F731BD', 1, 112, 0, 112, 112, 1, 111, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 21:05:13');
INSERT INTO `ims_stock_operation_log` VALUES (66, 1, 'CONFIRM_DEDUCT', 'ORDER', 'ORD1781528713367F731BD', -1, 112, 1, 111, 111, 0, 111, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 21:05:14');
INSERT INTO `ims_stock_operation_log` VALUES (67, 1, 'CONFIRM', 'ORDER', 'ORD1781528713367F731BD', -1, 112, 1, 111, 111, 0, 111, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 21:05:14');
INSERT INTO `ims_stock_operation_log` VALUES (68, 1, 'RESERVE', 'ORDER', 'P5-STOCK-20260615210514', 1, 111, 0, 111, 111, 1, 110, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 21:05:14');
INSERT INTO `ims_stock_operation_log` VALUES (69, 1, 'CANCEL_RESERVE', 'ORDER', 'P5-STOCK-20260615210514', 1, 111, 1, 110, 111, 0, 111, '释放预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 21:05:14');
INSERT INTO `ims_stock_operation_log` VALUES (70, 1, 'CANCEL', 'ORDER', 'P5-STOCK-20260615210514', 1, 111, 1, 110, 111, 0, 111, 'MQ取消预占落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 21:05:14');
INSERT INTO `ims_stock_operation_log` VALUES (71, 1, 'RESERVE', 'ORDER', 'P6-STOCK-20260615211510', 1, 111, 0, 111, 111, 1, 110, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 21:15:11');
INSERT INTO `ims_stock_operation_log` VALUES (72, 1, 'CANCEL_RESERVE', 'ORDER', 'P6-STOCK-20260615211510', 1, 111, 1, 110, 111, 0, 111, '释放预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-15 21:15:11');
INSERT INTO `ims_stock_operation_log` VALUES (73, 1, 'CANCEL', 'ORDER', 'P6-STOCK-20260615211510', 1, 111, 1, 110, 111, 0, 111, 'MQ取消预占落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-15 21:15:11');
INSERT INTO `ims_stock_operation_log` VALUES (74, 1, 'RESTORE', 'ORDER', 'P6-STOCK-20260615211510', 1, 111, 0, 111, 112, 0, 112, '回补库存', 'SYSTEM', NULL, 'system', 'ORDER_REFUND', '2026-06-15 21:15:11');
INSERT INTO `ims_stock_operation_log` VALUES (75, 1, 'RESERVE', 'ORDER', 'ORD17815421181506A2B36', 1, 112, 0, 112, 112, 1, 111, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-16 00:48:38');
INSERT INTO `ims_stock_operation_log` VALUES (76, 1, 'CONFIRM_DEDUCT', 'ORDER', 'ORD17815421181506A2B36', -1, 112, 1, 111, 111, 0, 111, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-16 00:48:38');
INSERT INTO `ims_stock_operation_log` VALUES (77, 1, 'CONFIRM', 'ORDER', 'ORD17815421181506A2B36', -1, 112, 1, 111, 111, 0, 111, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-16 00:48:38');
INSERT INTO `ims_stock_operation_log` VALUES (78, 1, 'RESERVE', 'ORDER', 'AUTO-STOCK-20260616004840', 1, 111, 0, 111, 111, 1, 110, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-16 00:48:40');
INSERT INTO `ims_stock_operation_log` VALUES (79, 1, 'CANCEL_RESERVE', 'ORDER', 'AUTO-STOCK-20260616004840', 1, 111, 1, 110, 111, 0, 111, '释放预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-16 00:48:40');
INSERT INTO `ims_stock_operation_log` VALUES (80, 1, 'CANCEL', 'ORDER', 'AUTO-STOCK-20260616004840', 1, 111, 1, 110, 111, 0, 111, 'MQ取消预占落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-16 00:48:40');
INSERT INTO `ims_stock_operation_log` VALUES (81, 1, 'RESERVE', 'ORDER', 'ORD17815425521398BF4BA', 1, 111, 0, 111, 111, 1, 110, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-16 00:55:52');
INSERT INTO `ims_stock_operation_log` VALUES (82, 1, 'CONFIRM_DEDUCT', 'ORDER', 'ORD17815425521398BF4BA', -1, 111, 1, 110, 110, 0, 110, '确认扣减库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-16 00:55:52');
INSERT INTO `ims_stock_operation_log` VALUES (83, 1, 'CONFIRM', 'ORDER', 'ORD17815425521398BF4BA', -1, 111, 1, 110, 110, 0, 110, 'MQ确认扣减落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-16 00:55:52');
INSERT INTO `ims_stock_operation_log` VALUES (84, 1, 'RESERVE', 'ORDER', 'AUTO-STOCK-20260616005554', 1, 110, 0, 110, 110, 1, 109, '预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-16 00:55:55');
INSERT INTO `ims_stock_operation_log` VALUES (85, 1, 'CANCEL_RESERVE', 'ORDER', 'AUTO-STOCK-20260616005554', 1, 110, 1, 109, 110, 0, 110, '释放预占库存', 'SYSTEM', NULL, 'system', 'ORDER_EVENT', '2026-06-16 00:55:55');
INSERT INTO `ims_stock_operation_log` VALUES (86, 1, 'CANCEL', 'ORDER', 'AUTO-STOCK-20260616005554', 1, 110, 1, 109, 110, 0, 110, 'MQ取消预占落库', 'SYSTEM', NULL, 'system', 'MQ_STOCK_SYNC', '2026-06-16 00:55:55');
INSERT INTO `ims_stock_operation_log` VALUES (87, 14, 'INIT', 'PRODUCT', '14', 0, 0, 0, 0, 0, 0, 0, '初始化库存', 'SYSTEM', NULL, 'system', 'PRODUCT_EVENT', '2026-06-19 14:16:53');
INSERT INTO `ims_stock_operation_log` VALUES (88, 13, 'INIT', 'PRODUCT', '15', 0, 0, 0, 0, 0, 0, 0, '初始化库存', 'SYSTEM', NULL, 'system', 'PRODUCT_EVENT', '2026-06-19 14:16:54');
INSERT INTO `ims_stock_operation_log` VALUES (89, 14, 'INIT', 'PRODUCT', '16', 1000, 0, 0, 0, 1000, 0, 1000, '初始化库存', 'SYSTEM', NULL, 'system', 'PRODUCT_EVENT', '2026-06-19 14:18:51');

-- ----------------------------
-- Table structure for ims_stock_reconciliation_record
-- ----------------------------
DROP TABLE IF EXISTS `ims_stock_reconciliation_record`;
CREATE TABLE `ims_stock_reconciliation_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '对账状态：CONSISTENT/INCONSISTENT/REPAIRED/IGNORED',
  `severity` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NONE' COMMENT '差异等级',
  `stock_snapshot_json` json NULL COMMENT '库存表快照',
  `expected_snapshot_json` json NULL COMMENT 'DB锁记录计算快照',
  `redis_snapshot_json` json NULL COMMENT 'Redis快照',
  `differences_json` json NULL COMMENT '差异明细',
  `repair_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NONE' COMMENT '修复状态：NONE/PENDING/DONE/IGNORED',
  `repair_remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '修复或忽略说明',
  `checked_at` datetime NOT NULL COMMENT '校验时间',
  `repaired_at` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_stock_reconciliation_sku_checked`(`sku_id` ASC, `checked_at` ASC) USING BTREE,
  INDEX `idx_stock_reconciliation_status_checked`(`status` ASC, `checked_at` ASC) USING BTREE,
  INDEX `idx_stock_reconciliation_repair_status`(`repair_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存对账记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ims_stock_reconciliation_record
-- ----------------------------
INSERT INTO `ims_stock_reconciliation_record` VALUES (1, 1, 'CONSISTENT', 'NONE', '{\"source\": \"STOCK_TABLE\", \"totalStock\": 121, \"lockedStock\": 0, \"availableStock\": 121}', '{\"source\": \"DB_LOCK_RECORD_CALCULATED\", \"totalStock\": 121, \"lockedStock\": 0, \"availableStock\": 121}', '{\"source\": \"REDIS\", \"totalStock\": 121, \"lockedStock\": 0, \"availableStock\": 121}', '[]', 'NONE', NULL, '2026-06-10 10:00:00', NULL, '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `ims_stock_reconciliation_record` VALUES (2, 2, 'REPAIRED', 'WARNING', '{\"source\": \"STOCK_TABLE\", \"totalStock\": 122, \"lockedStock\": 0, \"availableStock\": 122}', '{\"source\": \"DB_LOCK_RECORD_CALCULATED\", \"totalStock\": 122, \"lockedStock\": 1, \"availableStock\": 121}', '{\"source\": \"REDIS\", \"totalStock\": 122, \"lockedStock\": 1, \"availableStock\": 121}', '[\"库存表锁定库存与锁记录汇总不一致\", \"库存表可用库存与理论可用库存不一致\"]', 'DONE', '对账人员批量确认修复', '2026-06-10 10:00:00', '2026-06-10 16:52:19', '2026-06-10 10:00:00', '2026-06-10 16:52:19');
INSERT INTO `ims_stock_reconciliation_record` VALUES (3, 1, 'REPAIRED', 'WARNING', '{\"source\": \"STOCK_TABLE\", \"totalStock\": 121, \"lockedStock\": 0, \"availableStock\": 121}', '{\"source\": \"DB_LOCK_RECORD_CALCULATED\", \"totalStock\": 121, \"lockedStock\": 0, \"availableStock\": 121}', '{\"source\": \"REDIS\", \"totalStock\": 1898, \"lockedStock\": 0, \"availableStock\": 1898}', '[\"Redis总库存与DB理论库存不一致\", \"Redis可用库存与DB理论库存不一致\"]', 'DONE', '对账人员批量确认修复', '2026-06-10 02:45:00', '2026-06-10 16:52:19', '2026-06-10 02:45:00', '2026-06-10 16:52:19');
INSERT INTO `ims_stock_reconciliation_record` VALUES (4, 3, 'REPAIRED', 'WARNING', '{\"source\": \"STOCK_TABLE\", \"totalStock\": 123, \"lockedStock\": 0, \"availableStock\": 123}', '{\"source\": \"DB_LOCK_RECORD_CALCULATED\", \"totalStock\": 123, \"lockedStock\": 0, \"availableStock\": 123}', '{\"source\": \"REDIS\", \"totalStock\": 124, \"lockedStock\": 0, \"availableStock\": 124}', '[\"Redis总库存与DB理论库存不一致\", \"Redis可用库存与DB理论库存不一致\"]', 'DONE', '对账人员批量确认修复', '2026-06-10 02:45:00', '2026-06-10 16:52:19', '2026-06-10 02:45:00', '2026-06-10 16:52:19');
INSERT INTO `ims_stock_reconciliation_record` VALUES (5, 1, 'CONSISTENT', 'NONE', '{\"source\": \"STOCK_TABLE\", \"totalStock\": 121, \"lockedStock\": 0, \"availableStock\": 121}', '{\"source\": \"DB_LOCK_RECORD_CALCULATED\", \"totalStock\": 121, \"lockedStock\": 0, \"availableStock\": 121}', '{\"source\": \"REDIS\", \"totalStock\": 121, \"lockedStock\": 0, \"availableStock\": 121}', '[]', 'NONE', NULL, '2026-06-10 17:43:43', NULL, '2026-06-10 17:43:43', '2026-06-10 17:43:43');
INSERT INTO `ims_stock_reconciliation_record` VALUES (6, 1, 'REPAIRED', 'WARNING', '{\"source\": \"STOCK_TABLE\", \"totalStock\": 121, \"lockedStock\": 1, \"availableStock\": 120}', '{\"source\": \"DB_LOCK_RECORD_CALCULATED\", \"totalStock\": 121, \"lockedStock\": 0, \"availableStock\": 121}', '{\"source\": \"REDIS\", \"totalStock\": 121, \"lockedStock\": 0, \"availableStock\": 121}', '[\"库存表锁定库存与锁记录汇总不一致\", \"库存表可用库存与理论可用库存不一致\"]', 'DONE', '线上人工对账页触发库存修复', '2026-06-10 18:02:26', '2026-06-10 18:02:39', '2026-06-10 18:02:26', '2026-06-10 18:02:39');
INSERT INTO `ims_stock_reconciliation_record` VALUES (7, 1, 'CONSISTENT', 'NONE', '{\"source\": \"STOCK_TABLE\", \"totalStock\": 110, \"lockedStock\": 0, \"availableStock\": 110}', '{\"source\": \"DB_LOCK_RECORD_CALCULATED\", \"totalStock\": 110, \"lockedStock\": 0, \"availableStock\": 110}', '{\"source\": \"REDIS\", \"totalStock\": 110, \"lockedStock\": 0, \"availableStock\": 110}', '[]', 'NONE', '库存对账页触发批量修复', '2026-06-16 20:15:21', '2026-06-10 18:15:34', '2026-06-10 18:14:37', '2026-06-16 20:15:21');
INSERT INTO `ims_stock_reconciliation_record` VALUES (8, 2, 'REPAIRED', 'WARNING', '{\"source\": \"STOCK_TABLE\", \"totalStock\": 122, \"lockedStock\": 1, \"availableStock\": 121}', '{\"source\": \"DB_LOCK_RECORD_CALCULATED\", \"totalStock\": 122, \"lockedStock\": 0, \"availableStock\": 122}', '{\"source\": \"REDIS\", \"totalStock\": 122, \"lockedStock\": 0, \"availableStock\": 122}', '[\"库存表锁定库存与锁记录汇总不一致\", \"库存表可用库存与理论可用库存不一致\"]', 'DONE', '库存对账页触发批量修复', '2026-06-10 18:14:38', '2026-06-10 18:15:33', '2026-06-10 18:14:38', '2026-06-10 18:15:33');
INSERT INTO `ims_stock_reconciliation_record` VALUES (9, 3, 'CONSISTENT', 'NONE', '{\"source\": \"STOCK_TABLE\", \"totalStock\": 123, \"lockedStock\": 0, \"availableStock\": 123}', '{\"source\": \"DB_LOCK_RECORD_CALCULATED\", \"totalStock\": 123, \"lockedStock\": 0, \"availableStock\": 123}', '{\"source\": \"REDIS\", \"totalStock\": 123, \"lockedStock\": 0, \"availableStock\": 123}', '[]', 'NONE', NULL, '2026-06-10 18:14:42', NULL, '2026-06-10 18:14:42', '2026-06-10 18:14:42');
INSERT INTO `ims_stock_reconciliation_record` VALUES (10, 12, 'IGNORED', 'WARNING', '{\"source\": \"STOCK_TABLE\", \"totalStock\": 132, \"lockedStock\": 2, \"availableStock\": 130}', '{\"source\": \"DB_LOCK_RECORD_CALCULATED\", \"totalStock\": 132, \"lockedStock\": 0, \"availableStock\": 132}', '{\"source\": \"REDIS\", \"totalStock\": 132, \"lockedStock\": 0, \"availableStock\": 132}', '[\"库存表锁定库存与锁记录汇总不一致\", \"库存表可用库存与理论可用库存不一致\"]', 'IGNORED', 'P3 smoke ignore', '2026-06-10 20:01:25', '2026-06-15 20:32:03', '2026-06-10 20:01:25', '2026-06-15 20:32:03');

-- ----------------------------
-- Table structure for oms_order
-- ----------------------------
DROP TABLE IF EXISTS `oms_order`;
CREATE TABLE `oms_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `order_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单状态',
  `total_amount_cent` bigint NOT NULL COMMENT '订单总金额分',
  `pay_amount_cent` bigint NOT NULL COMMENT '实付金额分',
  `freight_amount_cent` bigint NOT NULL DEFAULT 0 COMMENT '运费分',
  `discount_amount_cent` bigint NOT NULL DEFAULT 0 COMMENT '优惠金额分',
  `receiver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收货人',
  `receiver_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收货电话',
  `receiver_province_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '省名称',
  `receiver_city_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '市名称',
  `receiver_district_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '区名称',
  `receiver_detail_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '详细地址',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '订单备注',
  `pay_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MOCK' COMMENT '支付类型',
  `paid_at` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `cancelled_at` datetime NULL DEFAULT NULL COMMENT '取消时间',
  `shipped_at` datetime NULL DEFAULT NULL COMMENT '发货时间',
  `completed_at` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `expire_time` datetime NOT NULL COMMENT '订单支付过期时间',
  `user_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '用户侧是否删除：0-否，1-是',
  `user_deleted_at` datetime NULL DEFAULT NULL COMMENT '用户侧删除时间',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_oms_order_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_oms_order_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_oms_order_status_id`(`order_status` ASC, `id` ASC) USING BTREE,
  INDEX `idx_oms_order_user_id_id`(`user_id` ASC, `id` ASC) USING BTREE,
  INDEX `idx_oms_order_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_oms_order_receiver_phone`(`receiver_phone` ASC) USING BTREE,
  INDEX `idx_oms_order_user_deleted`(`user_id` ASC, `user_deleted` ASC, `id` ASC) USING BTREE,
  INDEX `idx_oms_order_status_expire_time`(`order_status` ASC, `expire_time` ASC) USING BTREE,
  INDEX `idx_oms_order_status_expire_order_no`(`order_status` ASC, `expire_time` ASC, `order_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of oms_order
-- ----------------------------
INSERT INTO `oms_order` VALUES (1, 'RCT202606100001', 1, 'PAID', 10000, 10000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', 'CASE01 正常支付成功：订单已支付/支付单SUCCESS/金额一致/渠道成功', 'MOCK', '2026-06-10 09:01:00', NULL, NULL, NULL, '2026-06-10 12:00:00', 0, NULL, 0, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order` VALUES (2, 'RCT202606100002', 1, 'TIMEOUT_CANCELLED', 12000, 12000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', 'CASE02 待支付：支付单PENDING，渠道未支付，不应判长短款', 'MOCK', NULL, '2026-06-10 15:13:54', NULL, NULL, '2026-06-10 12:00:00', 0, NULL, 1, '2026-06-10 10:00:00', '2026-06-10 15:13:53', NULL);
INSERT INTO `oms_order` VALUES (3, 'RCT202606100003', 1, 'PAID', 13000, 13000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', 'CASE03 支付单SUCCESS且订单已同步为已支付', 'MOCK', '2026-06-10 10:00:00', NULL, NULL, NULL, '2026-06-10 12:00:00', 0, NULL, 1, '2026-06-10 10:00:00', '2026-06-10 15:13:53', NULL);
INSERT INTO `oms_order` VALUES (4, 'RCT202606100004', 1, 'TIMEOUT_CANCELLED', 14000, 14000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', 'CASE04 本地PAYING但渠道SUCCESS：同步支付状态+补偿订单', 'MOCK', NULL, '2026-06-10 15:13:54', NULL, NULL, '2026-06-10 12:00:00', 0, NULL, 1, '2026-06-10 10:00:00', '2026-06-10 15:13:53', NULL);
INSERT INTO `oms_order` VALUES (5, 'RCT202606100005', 1, 'PAID', 15000, 15000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', 'CASE05 平台成功但渠道未确认：长款/待渠道调单/恢复待支付/关闭释放/补款', 'MOCK', '2026-06-10 09:05:00', NULL, NULL, NULL, '2026-06-10 12:00:00', 0, NULL, 0, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order` VALUES (6, 'RCT202606100006', 1, 'PAID', 16000, 16000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', 'CASE06 订单已支付但缺少支付单：人工审核/恢复待支付/关闭释放库存', 'MOCK', '2026-06-10 09:06:00', NULL, NULL, NULL, '2026-06-10 12:00:00', 0, NULL, 0, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order` VALUES (7, 'RCT202606100007', 1, 'PAID', 17000, 17000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', 'CASE07 金额不一致：待退款/补款/人工审核', 'MOCK', '2026-06-10 09:07:00', NULL, NULL, NULL, '2026-06-10 12:00:00', 0, NULL, 0, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order` VALUES (8, 'RCT202606100008', 1, 'REFUNDED', 18000, 18000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', 'CASE08 有退款单：退款同步/退款对账/退款状态金额不一致', 'MOCK', '2026-06-10 09:08:00', '2026-06-10 15:18:18', NULL, '2026-06-10 15:18:18', '2026-06-10 12:00:00', 0, NULL, 1, '2026-06-10 10:00:00', '2026-06-10 15:18:17', NULL);
INSERT INTO `oms_order` VALUES (9, 'RCT202606100009', 1, 'REFUNDED', 19000, 19000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', 'CASE09 支付异常处理：关闭释放库存/恢复待支付/人工确认已支付 | ADMIN_CONFIRM_PAID:PAY_REPAIR: 支付单已成功，后台补偿订单支付状态 | PAYMENT_EXCEPTION_FROM_PAYMENT_EXCEPTION | ADMIN_CONFIRM_PAID:PAY_REPAIR: 支付单已成功，后台补偿订单支付状态', 'MOCK', '2026-06-10 15:13:58', '2026-06-10 15:54:47', NULL, '2026-06-10 15:54:47', '2026-06-10 12:00:00', 0, NULL, 5, '2026-06-10 10:00:00', '2026-06-10 15:54:47', NULL);
INSERT INTO `oms_order` VALUES (10, 'RCT202606100010', 1, 'SHIPPED', 20000, 20000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', 'CASE10 已发货且支付对平：重新对账仍一致', 'MOCK', '2026-06-10 09:10:00', NULL, '2026-06-10 09:30:00', NULL, '2026-06-10 12:00:00', 0, NULL, 0, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order` VALUES (11, 'ORD17810335613035A1625', 1, 'TIMEOUT_CANCELLED', 22000, 22000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', '', 'MOCK', NULL, '2026-06-10 03:47:41', NULL, NULL, '2026-06-10 03:47:41', 0, NULL, 1, '2026-06-10 03:32:41', '2026-06-10 03:47:41', NULL);
INSERT INTO `oms_order` VALUES (12, 'ORD1781075976142E01526', 1, 'COMPLETED', 21000, 21000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', '', 'MOCK', '2026-06-10 15:19:40', NULL, NULL, '2026-06-10 15:19:43', '2026-06-10 15:34:36', 1, '2026-06-10 15:23:19', 2, '2026-06-10 15:19:35', '2026-06-10 15:23:19', NULL);
INSERT INTO `oms_order` VALUES (13, 'ORD1781076001361016574', 1, 'SHIPPED', 21000, 21000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', 'REFUND_REJECTED:商家审核驳回 | PAYMENT_EXCEPTION_FROM_PAYMENT_EXCEPTION | ADMIN_CONFIRM_PAID:PAY_REPAIR: 支付单已成功，后台补偿订单支付状态 | PAYMENT_EXCEPTION_FROM_PAYMENT_EXCEPTION | ADMIN_CONFIRM_PAID:PAY_REPAIR: 支付单已成功，后台补偿订单支付状态', 'MOCK', '2026-06-10 15:20:06', NULL, '2026-06-10 16:30:42', NULL, '2026-06-10 15:35:01', 0, NULL, 9, '2026-06-10 15:20:01', '2026-06-10 16:32:03', NULL);
INSERT INTO `oms_order` VALUES (14, 'ORD17810762227909DD3A7', 1, 'CANCELLED', 18000, 18000, 0, 0, '林先生', '13910002001', '北京市', '北京市', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', 'PAYMENT_EXCEPTION_FROM_PAYMENT_EXCEPTION | RESTORE_PENDING_PAYMENT | PAYMENT_EXCEPTION_FROM_PAYMENT_EXCEPTION | RESTORE_PENDING_PAYMENT', 'MOCK', NULL, '2026-06-10 15:27:21', NULL, NULL, '2026-06-10 15:38:43', 0, NULL, 5, '2026-06-10 15:23:42', '2026-06-10 15:27:20', NULL);
INSERT INTO `oms_order` VALUES (15, 'ORD17810814566188EA786', 1, 'CANCELLED', 22000, 22000, 0, 0, '艾米莉', '13910002111', '广东省', '佛山市', '南海区', '狮山镇罗村碧翠公馆', 'PAYMENT_EXCEPTION_FROM_PAYMENT_EXCEPTION | RESTORE_PENDING_PAYMENT', 'MOCK', NULL, '2026-06-10 16:51:49', NULL, NULL, '2026-06-10 17:06:34', 0, NULL, 3, '2026-06-10 16:50:56', '2026-06-10 16:51:48', NULL);
INSERT INTO `oms_order` VALUES (16, 'ORD17812006903496B061E', 1, 'REFUND_PENDING', 21000, 21000, 0, 0, '艾米莉', '13910002111', '广东省', '佛山市', '南海区', '狮山镇罗村碧翠公馆', '', 'MOCK', '2026-06-12 01:58:14', NULL, NULL, NULL, '2026-06-12 02:13:10', 0, NULL, 2, '2026-06-12 01:58:11', '2026-06-12 01:58:19', NULL);
INSERT INTO `oms_order` VALUES (17, 'ORD178151990140736FC43', 1, 'COMPLETED', 10000, 10000, 0, 0, 'Auto Test User', '13800000000', 'Beijing', 'Beijing', 'Chaoyang', 'Auto Test Address No.1', 'e2e business flow test', 'MOCK', '2026-06-15 18:38:22', NULL, '2026-06-15 18:38:22', '2026-06-15 18:38:22', '2026-06-15 18:53:21', 0, NULL, 3, '2026-06-15 18:38:21', '2026-06-15 18:38:22', NULL);
INSERT INTO `oms_order` VALUES (18, 'ORD17815228604692C7570', 1, 'COMPLETED', 10000, 10000, 0, 0, 'Auto Test User', '13800000000', 'Beijing', 'Beijing', 'Chaoyang', 'Auto Test Address No.1', 'p1 business flow test', 'MOCK', '2026-06-15 19:27:41', NULL, '2026-06-15 19:27:43', '2026-06-15 19:27:43', '2026-06-15 19:42:40', 0, NULL, 3, '2026-06-15 19:27:40', '2026-06-15 19:27:43', NULL);
INSERT INTO `oms_order` VALUES (19, 'ORD178152381362132B276', 1, 'PAYMENT_EXCEPTION', 10000, 10000, 0, 0, 'P2 Auto User', '13800000000', 'Beijing', 'Beijing', 'Chaoyang', 'P2 Auto Address', 'p2 business flow test | PAYMENT_EXCEPTION_FROM_PAYMENT_EXCEPTION:P2 smoke mark', 'MOCK', '2026-06-15 19:43:34', NULL, NULL, NULL, '2026-06-15 19:58:34', 0, NULL, 2, '2026-06-15 19:43:33', '2026-06-15 19:43:35', NULL);
INSERT INTO `oms_order` VALUES (20, 'ORD17815238604099B7C62', 1, 'PAYMENT_EXCEPTION', 10000, 10000, 0, 0, 'P2 Auto User', '13800000000', 'Beijing', 'Beijing', 'Chaoyang', 'P2 Auto Address', 'p2 business flow test | PAYMENT_EXCEPTION_FROM_PAYMENT_EXCEPTION:P2 smoke mark', 'MOCK', '2026-06-15 19:44:21', NULL, NULL, NULL, '2026-06-15 19:59:20', 0, NULL, 2, '2026-06-15 19:44:20', '2026-06-15 19:44:21', NULL);
INSERT INTO `oms_order` VALUES (21, 'ORD1781523940797C6EC98', 1, 'PAYMENT_EXCEPTION', 10000, 10000, 0, 0, 'P2 Auto User', '13800000000', 'Beijing', 'Beijing', 'Chaoyang', 'P2 Auto Address', 'p2 business flow test | PAYMENT_EXCEPTION_FROM_PAYMENT_EXCEPTION:P2 smoke mark', 'MOCK', '2026-06-15 19:45:41', NULL, NULL, NULL, '2026-06-15 20:00:41', 0, NULL, 2, '2026-06-15 19:45:40', '2026-06-15 19:45:41', NULL);
INSERT INTO `oms_order` VALUES (22, 'ORD1781524329822DF7ACD', 1, 'PAYMENT_EXCEPTION', 10000, 10000, 0, 0, 'P2 Auto User', '13800000000', 'Beijing', 'Beijing', 'Chaoyang', 'P2 Auto Address', 'p2 business flow test | PAYMENT_EXCEPTION_FROM_PAYMENT_EXCEPTION:P2 smoke mark', 'MOCK', '2026-06-15 19:52:10', NULL, NULL, NULL, '2026-06-15 20:07:10', 0, NULL, 2, '2026-06-15 19:52:09', '2026-06-15 19:52:10', NULL);
INSERT INTO `oms_order` VALUES (23, 'ORD178152439679881F09C', 1, 'PAYMENT_EXCEPTION', 10000, 10000, 0, 0, 'P2 Auto User', '13800000000', 'Beijing', 'Beijing', 'Chaoyang', 'P2 Auto Address', 'p2 business flow test | PAYMENT_EXCEPTION_FROM_PAYMENT_EXCEPTION:P2 smoke mark', 'MOCK', '2026-06-15 19:53:17', NULL, NULL, NULL, '2026-06-15 20:08:17', 0, NULL, 2, '2026-06-15 19:53:16', '2026-06-15 19:53:18', NULL);
INSERT INTO `oms_order` VALUES (24, 'ORD1781524417974BA4CFF', 1, 'COMPLETED', 57000, 57000, 0, 0, '艾米莉', '13910002111', '广东省', '佛山市', '南海区', '狮山镇罗村碧翠公馆', '', 'MOCK', '2026-06-15 19:53:44', NULL, '2026-06-15 19:54:05', '2026-06-15 19:54:09', '2026-06-15 20:08:38', 0, NULL, 3, '2026-06-15 19:53:38', '2026-06-15 19:54:09', NULL);
INSERT INTO `oms_order` VALUES (25, 'ORD1781526648573A839F8', 1, 'REFUND_PENDING', 10000, 10000, 0, 0, 'P3 Auto User', '13800000000', 'Beijing', 'Beijing', 'Chaoyang', 'P3 Auto Address', 'p3 smoke | REFUND_PENDING:支付退款处理中', 'MOCK', '2026-06-15 20:30:49', NULL, NULL, NULL, '2026-06-15 20:45:49', 0, NULL, 2, '2026-06-15 20:30:48', '2026-06-15 20:30:49', NULL);
INSERT INTO `oms_order` VALUES (26, 'ORD178152672263850B15C', 1, 'REFUND_PENDING', 10000, 10000, 0, 0, 'P3 Auto User', '13800000000', 'Beijing', 'Beijing', 'Chaoyang', 'P3 Auto Address', 'p3 smoke | REFUND_PENDING:支付退款处理中', 'MOCK', '2026-06-15 20:32:03', NULL, NULL, NULL, '2026-06-15 20:47:03', 0, NULL, 2, '2026-06-15 20:32:02', '2026-06-15 20:32:03', NULL);
INSERT INTO `oms_order` VALUES (27, 'ORD178152809766513D1B9', 1, 'REFUND_PENDING', 10000, 10000, 0, 0, 'P4 User', '13800000000', 'Beijing', 'Beijing', 'Chaoyang', 'P4 Address', 'p4 | REFUND_PENDING:支付退款处理中', 'MOCK', '2026-06-15 20:54:58', NULL, NULL, NULL, '2026-06-15 21:09:58', 0, NULL, 2, '2026-06-15 20:54:57', '2026-06-15 20:54:59', NULL);
INSERT INTO `oms_order` VALUES (28, 'ORD1781528713367F731BD', 1, 'REFUND_PENDING', 10000, 10000, 0, 0, 'P5 User', '13800000000', 'Beijing', 'Beijing', 'Chaoyang', 'P5 Address', 'p5 | REFUND_PENDING:支付退款处理中', 'MOCK', '2026-06-15 21:05:14', NULL, '2026-06-15 21:05:14', '2026-06-15 21:05:14', '2026-06-15 21:20:13', 0, NULL, 4, '2026-06-15 21:05:13', '2026-06-15 21:05:14', NULL);
INSERT INTO `oms_order` VALUES (29, 'ORD17815421181506A2B36', 1, 'COMPLETED', 10000, 10000, 0, 0, 'Auto Test User', '13800000000', 'Beijing', 'Beijing', 'Chaoyang', 'Auto Test Address No.1', 'p1 business flow test', 'MOCK', '2026-06-16 00:48:38', NULL, '2026-06-16 00:48:41', '2026-06-16 00:48:41', '2026-06-16 01:03:38', 0, NULL, 3, '2026-06-16 00:48:38', '2026-06-16 00:48:41', NULL);
INSERT INTO `oms_order` VALUES (30, 'ORD17815425521398BF4BA', 1, 'COMPLETED', 10000, 10000, 0, 0, 'Auto Test User', '13800000000', 'Beijing', 'Beijing', 'Chaoyang', 'Auto Test Address No.1', 'p1 business flow test', 'MOCK', '2026-06-16 00:55:52', NULL, '2026-06-16 00:55:55', '2026-06-16 00:55:55', '2026-06-16 01:10:52', 0, NULL, 3, '2026-06-16 00:55:52', '2026-06-16 00:55:55', NULL);

-- ----------------------------
-- Table structure for oms_order_item
-- ----------------------------
DROP TABLE IF EXISTS `oms_order_item`;
CREATE TABLE `oms_order_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `spu_id` bigint NOT NULL COMMENT 'SPU ID',
  `sku_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SKU名称快照',
  `sku_image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'SKU图片快照',
  `sale_price_cent` bigint NOT NULL COMMENT '成交单价分',
  `quantity` int NOT NULL COMMENT '购买数量',
  `total_amount_cent` bigint NOT NULL COMMENT '总金额分',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_oms_order_item_order_id`(`order_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单项表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of oms_order_item
-- ----------------------------
INSERT INTO `oms_order_item` VALUES (1, 1, 'RCT202606100001', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order_item` VALUES (2, 2, 'RCT202606100002', 2, 2, '鹿岛空气炸锅 默认规格', '/uploads/product/2026/06/10/a83bd60d6f1f4bba818788d9f99210f6.png', 12000, 1, 12000, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order_item` VALUES (3, 3, 'RCT202606100003', 3, 3, '青橙护眼台灯 默认规格', '/uploads/product/2026/06/10/48e6e507f29f475790e6c1a459270605.png', 13000, 1, 13000, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order_item` VALUES (4, 4, 'RCT202606100004', 4, 4, '北庭跑步鞋 默认规格', '/uploads/product/2026/06/10/59908e0eff0c4e7888e7145c44c4cf28.jpeg', 14000, 1, 14000, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order_item` VALUES (5, 5, 'RCT202606100005', 5, 5, '云岚双肩包 默认规格', '/uploads/product/2026/06/10/3ea59cfc5e744be58ef88bde491c5d78.jpeg', 15000, 1, 15000, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order_item` VALUES (6, 6, 'RCT202606100006', 6, 6, '山也电动牙刷 默认规格', '/uploads/product/2026/06/10/2741559b83754426bc90663c1c65c04f.jpeg', 16000, 1, 16000, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order_item` VALUES (7, 7, 'RCT202606100007', 7, 7, '沐白乳胶枕 默认规格', '/uploads/product/2026/06/10/33b22da0a55b4e5ba0bf285ada3f866e.jpeg', 17000, 1, 17000, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order_item` VALUES (8, 8, 'RCT202606100008', 8, 8, '岚森牛奶礼盒 默认规格', '/uploads/product/2026/06/10/ece29ebb73a043138b45b5660aa721a3.jpeg', 18000, 1, 18000, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order_item` VALUES (9, 9, 'RCT202606100009', 9, 9, '初合机械键盘 默认规格', '/uploads/product/2026/06/10/3443574991734406bb2f7bfe60b86a8e.jpeg', 19000, 1, 19000, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order_item` VALUES (10, 10, 'RCT202606100010', 10, 10, '鲸选防晒霜 默认规格', '/uploads/product/2026/06/10/d72a34d879a14db3b42abe9e310519bd.jpeg', 20000, 1, 20000, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `oms_order_item` VALUES (11, 11, 'ORD17810335613035A1625', 12, 12, '松禾无线耳机 默认规格', '/uploads/product/2026/06/10/562c08ac753e4bb0a7bbc7d6eabe7d9c.jpg', 22000, 1, 22000, '2026-06-10 03:32:41', '2026-06-10 03:32:41', NULL);
INSERT INTO `oms_order_item` VALUES (12, 12, 'ORD1781075976142E01526', 11, 11, '有栖行李箱 默认规格', '/uploads/product/2026/06/10/a5ac2983f753438f9ad6cd35239fa671.png', 21000, 1, 21000, '2026-06-10 15:19:35', '2026-06-10 15:19:35', NULL);
INSERT INTO `oms_order_item` VALUES (13, 13, 'ORD1781076001361016574', 11, 11, '有栖行李箱 默认规格', '/uploads/product/2026/06/10/a5ac2983f753438f9ad6cd35239fa671.png', 21000, 1, 21000, '2026-06-10 15:20:01', '2026-06-10 15:20:01', NULL);
INSERT INTO `oms_order_item` VALUES (14, 14, 'ORD17810762227909DD3A7', 8, 8, '岚森牛奶礼盒 默认规格', '/uploads/product/2026/06/10/ece29ebb73a043138b45b5660aa721a3.jpeg', 18000, 1, 18000, '2026-06-10 15:23:42', '2026-06-10 15:23:42', NULL);
INSERT INTO `oms_order_item` VALUES (15, 15, 'ORD17810814566188EA786', 12, 12, '松禾无线耳机 默认规格', '/uploads/product/2026/06/10/562c08ac753e4bb0a7bbc7d6eabe7d9c.jpg', 22000, 1, 22000, '2026-06-10 16:50:56', '2026-06-10 16:50:56', NULL);
INSERT INTO `oms_order_item` VALUES (16, 16, 'ORD17812006903496B061E', 11, 11, '有栖行李箱 默认规格', '/uploads/product/2026/06/10/a5ac2983f753438f9ad6cd35239fa671.png', 21000, 1, 21000, '2026-06-12 01:58:11', '2026-06-12 01:58:11', NULL);
INSERT INTO `oms_order_item` VALUES (17, 17, 'ORD178151990140736FC43', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-15 18:38:21', '2026-06-15 18:38:21', NULL);
INSERT INTO `oms_order_item` VALUES (18, 18, 'ORD17815228604692C7570', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-15 19:27:40', '2026-06-15 19:27:40', NULL);
INSERT INTO `oms_order_item` VALUES (19, 19, 'ORD178152381362132B276', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-15 19:43:33', '2026-06-15 19:43:33', NULL);
INSERT INTO `oms_order_item` VALUES (20, 20, 'ORD17815238604099B7C62', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-15 19:44:20', '2026-06-15 19:44:20', NULL);
INSERT INTO `oms_order_item` VALUES (21, 21, 'ORD1781523940797C6EC98', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-15 19:45:40', '2026-06-15 19:45:40', NULL);
INSERT INTO `oms_order_item` VALUES (22, 22, 'ORD1781524329822DF7ACD', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-15 19:52:09', '2026-06-15 19:52:09', NULL);
INSERT INTO `oms_order_item` VALUES (23, 23, 'ORD178152439679881F09C', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-15 19:53:16', '2026-06-15 19:53:16', NULL);
INSERT INTO `oms_order_item` VALUES (24, 24, 'ORD1781524417974BA4CFF', 3, 3, '青橙护眼台灯 默认规格', '/uploads/product/2026/06/10/48e6e507f29f475790e6c1a459270605.png', 13000, 1, 13000, '2026-06-15 19:53:38', '2026-06-15 19:53:38', NULL);
INSERT INTO `oms_order_item` VALUES (25, 24, 'ORD1781524417974BA4CFF', 12, 12, '松禾无线耳机 默认规格', '/uploads/product/2026/06/10/562c08ac753e4bb0a7bbc7d6eabe7d9c.jpg', 22000, 2, 44000, '2026-06-15 19:53:38', '2026-06-15 19:53:38', NULL);
INSERT INTO `oms_order_item` VALUES (26, 25, 'ORD1781526648573A839F8', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-15 20:30:48', '2026-06-15 20:30:48', NULL);
INSERT INTO `oms_order_item` VALUES (27, 26, 'ORD178152672263850B15C', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-15 20:32:02', '2026-06-15 20:32:02', NULL);
INSERT INTO `oms_order_item` VALUES (28, 27, 'ORD178152809766513D1B9', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-15 20:54:57', '2026-06-15 20:54:57', NULL);
INSERT INTO `oms_order_item` VALUES (29, 28, 'ORD1781528713367F731BD', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-15 21:05:13', '2026-06-15 21:05:13', NULL);
INSERT INTO `oms_order_item` VALUES (30, 29, 'ORD17815421181506A2B36', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-16 00:48:38', '2026-06-16 00:48:38', NULL);
INSERT INTO `oms_order_item` VALUES (31, 30, 'ORD17815425521398BF4BA', 1, 1, '星麦保温杯 默认规格', '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', 10000, 1, 10000, '2026-06-16 00:55:52', '2026-06-16 00:55:52', NULL);

-- ----------------------------
-- Table structure for oms_order_refund
-- ----------------------------
DROP TABLE IF EXISTS `oms_order_refund`;
CREATE TABLE `oms_order_refund`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '退款申请ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `refund_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '退款单号',
  `refund_amount_cent` bigint NULL DEFAULT NULL COMMENT '退款金额，单位分',
  `channel_refund_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '渠道退款流水号',
  `refund_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '退款状态',
  `refund_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '退款原因',
  `fail_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_oms_order_refund_refund_no`(`refund_no` ASC) USING BTREE,
  INDEX `idx_oms_order_refund_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_oms_order_refund_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_oms_order_refund_status`(`refund_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单退款申请表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of oms_order_refund
-- ----------------------------
INSERT INTO `oms_order_refund` VALUES (1, 8, 'RCT202606100008', 1, 'RFDRCT202606100008', 18000, 'MOCK-REFUND-RFDRCT202606100008', 'REFUND_SUCCESS', '对账退款测试', '', '2026-06-10 10:00:00', '2026-06-10 15:18:18');
INSERT INTO `oms_order_refund` VALUES (2, 9, 'RCT202606100009', 1, 'ORF1781078073650ED3D88', 19000, 'MOCK-REFUND-ORF1781078073650ED3D88', 'REFUND_SUCCESS', '...', '', '2026-06-10 15:54:33', '2026-06-10 15:54:47');
INSERT INTO `oms_order_refund` VALUES (3, 16, 'ORD17812006903496B061E', 1, 'ORF17812007111280C0DE2', 21000, '', 'REFUNDING', '...', '', '2026-06-12 01:58:31', '2026-06-12 01:58:31');
INSERT INTO `oms_order_refund` VALUES (4, 25, 'ORD1781526648573A839F8', 1, 'ORF1781526648822FD054D', 10000, '', 'REFUNDING', '商品与描述不符', '', '2026-06-15 20:30:48', '2026-06-15 20:30:49');
INSERT INTO `oms_order_refund` VALUES (5, 26, 'ORD178152672263850B15C', 1, 'ORF178152672284164F2E8', 10000, '', 'REFUNDING', '商品与描述不符', '', '2026-06-15 20:32:02', '2026-06-15 20:32:03');
INSERT INTO `oms_order_refund` VALUES (6, 27, 'ORD178152809766513D1B9', 1, 'ORF1781528098611D0788A', 10000, '', 'REFUNDING', 'P4 refund', '', '2026-06-15 20:54:58', '2026-06-15 20:54:59');
INSERT INTO `oms_order_refund` VALUES (7, 28, 'ORD1781528713367F731BD', 1, 'ORF1781528713716FCFAFF', 10000, '', 'REFUNDING', '商品与描述不符', '', '2026-06-15 21:05:13', '2026-06-15 21:05:14');

-- ----------------------------
-- Table structure for oms_order_refund_item
-- ----------------------------
DROP TABLE IF EXISTS `oms_order_refund_item`;
CREATE TABLE `oms_order_refund_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `refund_id` bigint NOT NULL COMMENT '退款记录ID',
  `refund_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '退款单号',
  `order_item_id` bigint NOT NULL COMMENT '订单明细ID',
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `quantity` int NOT NULL COMMENT '退款数量',
  `refund_amount_cent` bigint NOT NULL COMMENT '退款金额，单位分',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_oms_order_refund_item_refund_no`(`refund_no` ASC) USING BTREE,
  INDEX `idx_oms_order_refund_item_order_item_id`(`order_item_id` ASC) USING BTREE,
  INDEX `idx_oms_order_refund_item_sku_id`(`sku_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单退款明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of oms_order_refund_item
-- ----------------------------
INSERT INTO `oms_order_refund_item` VALUES (1, 1, 'RFDRCT202606100008', 8, 8, 1, 18000);
INSERT INTO `oms_order_refund_item` VALUES (2, 2, 'ORF1781078073650ED3D88', 9, 9, 1, 19000);
INSERT INTO `oms_order_refund_item` VALUES (3, 3, 'ORF17812007111280C0DE2', 16, 11, 1, 21000);
INSERT INTO `oms_order_refund_item` VALUES (4, 4, 'ORF1781526648822FD054D', 26, 1, 1, 10000);
INSERT INTO `oms_order_refund_item` VALUES (5, 5, 'ORF178152672284164F2E8', 27, 1, 1, 10000);
INSERT INTO `oms_order_refund_item` VALUES (6, 6, 'ORF1781528098611D0788A', 28, 1, 1, 10000);
INSERT INTO `oms_order_refund_item` VALUES (7, 7, 'ORF1781528713716FCFAFF', 29, 1, 1, 10000);

-- ----------------------------
-- Table structure for pay_callback_record
-- ----------------------------
DROP TABLE IF EXISTS `pay_callback_record`;
CREATE TABLE `pay_callback_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '回调记录ID',
  `channel` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付渠道',
  `callback_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PAY' COMMENT '回调类型：PAY/REFUND',
  `pay_order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付单号',
  `refund_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '退款单号',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '订单号',
  `out_trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户订单号',
  `transaction_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方交易号',
  `amount_cent` bigint NULL DEFAULT NULL COMMENT '金额分',
  `trade_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '渠道交易状态',
  `signature` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '签名信息',
  `verified` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否验签通过',
  `process_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'INIT' COMMENT '处理状态',
  `fail_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `raw_payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '原始回调报文',
  `callback_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回调时间',
  `processed_at` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pay_callback_record_channel`(`channel` ASC) USING BTREE,
  INDEX `idx_pay_callback_record_pay_order_no`(`pay_order_no` ASC) USING BTREE,
  INDEX `idx_pay_callback_record_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_pay_callback_record_out_trade_no`(`out_trade_no` ASC) USING BTREE,
  INDEX `idx_pay_callback_record_refund_no`(`refund_no` ASC) USING BTREE,
  INDEX `idx_pay_callback_record_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付回调记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pay_callback_record
-- ----------------------------
INSERT INTO `pay_callback_record` VALUES (1, 'MOCK', 'PAY', 'PAYRCT202606100001', NULL, 'RCT202606100001', 'PAYRCT202606100001', 'MOCK-TXN-RCT-001', 10000, 'SUCCESS', 'mock-signature', 1, 'PROCESSED', '', '{\"case\":\"normal_success\"}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `pay_callback_record` VALUES (2, 'MOCK', 'PAY', 'PAYRCT202606100003', NULL, 'RCT202606100003', 'PAYRCT202606100003', 'MOCK-TXN-RCT-003', 13000, 'SUCCESS', 'mock-signature', 1, 'PROCESSED', '', '{\"case\":\"pay_success_order_not_synced\"}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `pay_callback_record` VALUES (3, 'MOCK', 'PAY', 'PAYRCT202606100004', NULL, 'RCT202606100004', 'PAYRCT202606100004', 'MOCK-TXN-RCT-004', 14000, 'SUCCESS', 'mock-signature', 1, 'RECEIVED', '渠道已成功，本地支付单仍PAYING', '{\"case\":\"local_paying_channel_success\"}', '2026-06-10 10:00:00', NULL, '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `pay_callback_record` VALUES (4, 'MOCK', 'PAY', 'PAYRCT202606100005', NULL, 'RCT202606100005', 'PAYRCT202606100005', '', 15000, 'NOT_FOUND', 'mock-signature', 1, 'RECEIVED', '渠道未查到成功交易', '{\"case\":\"platform_success_channel_missing\"}', '2026-06-10 10:00:00', NULL, '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `pay_callback_record` VALUES (5, 'MOCK', 'PAY', 'PAYRCT202606100007', NULL, 'RCT202606100007', 'PAYRCT202606100007', 'MOCK-TXN-RCT-007', 17000, 'SUCCESS', 'mock-signature', 1, 'PROCESSED', '', '{\"case\":\"pay_success_consistent\"}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `pay_callback_record` VALUES (6, 'MOCK', 'REFUND', 'PAYRCT202606100008', 'RFDRCT202606100008', 'RCT202606100008', 'RFDRCT202606100008', 'MOCK-REFUND-RFDRCT202606100008', 18000, 'REFUND_SUCCESS', 'mock-signature', 1, 'PROCESSED', '', '{\"case\":\"refund_success_consistent\"}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `pay_callback_record` VALUES (7, 'MOCK', 'PAY', 'PAY1781075978484B35E89', NULL, 'ORD1781075976142E01526', 'PAY1781075978484B35E89', 'MOCK-ORD1781075976142E01526', 21000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD1781075976142E01526\",\"payOrderNo\":\"PAY1781075978484B35E89\",\"transactionNo\":\"MOCK-ORD1781075976142E01526\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":21000}', '2026-06-10 15:19:40', '2026-06-10 15:19:40', '2026-06-10 15:19:40', '2026-06-10 15:19:40');
INSERT INTO `pay_callback_record` VALUES (8, 'MOCK', 'PAY', 'PAY1781076004337B352E6', NULL, 'ORD1781076001361016574', 'PAY1781076004337B352E6', 'MOCK-ORD1781076001361016574', 21000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD1781076001361016574\",\"payOrderNo\":\"PAY1781076004337B352E6\",\"transactionNo\":\"MOCK-ORD1781076001361016574\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":21000}', '2026-06-10 15:20:06', '2026-06-10 15:20:06', '2026-06-10 15:20:06', '2026-06-10 15:20:06');
INSERT INTO `pay_callback_record` VALUES (9, 'MOCK', 'PAY', 'PAYRCT202606100008', NULL, 'RCT202606100008', 'PAYRCT202606100008', 'MOCK-TXN-RCT-008', 18000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"callbackType\":\"PAY\",\"orderNo\":\"RCT202606100008\",\"payOrderNo\":\"PAYRCT202606100008\",\"transactionNo\":\"MOCK-TXN-RCT-008\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":18000,\"case\":\"refund_order_origin_pay_success\"}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `pay_callback_record` VALUES (10, 'MOCK', 'PAY', 'PAYRCT202606100009', NULL, 'RCT202606100009', 'PAYRCT202606100009', 'MOCK-TXN-RCT-009', 19000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"callbackType\":\"PAY\",\"orderNo\":\"RCT202606100009\",\"payOrderNo\":\"PAYRCT202606100009\",\"transactionNo\":\"MOCK-TXN-RCT-009\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":19000,\"case\":\"refund_order_origin_pay_success\"}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `pay_callback_record` VALUES (11, 'MOCK', 'PAY', 'PAYRCT202606100010', NULL, 'RCT202606100010', 'PAYRCT202606100010', 'MOCK-TXN-RCT-010', 20000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"callbackType\":\"PAY\",\"orderNo\":\"RCT202606100010\",\"payOrderNo\":\"PAYRCT202606100010\",\"transactionNo\":\"MOCK-TXN-RCT-010\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":20000,\"case\":\"normal_shipped_pay_success\"}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `pay_callback_record` VALUES (12, 'MOCK', 'REFUND', 'PAYRCT202606100008', 'RFDRCT202606100008', 'RCT202606100008', 'RFDRCT202606100008', 'MOCK-REFUND-RFDRCT202606100008', 18000, 'REFUND_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"callbackType\":\"REFUND\",\"orderNo\":\"RCT202606100008\",\"payOrderNo\":\"PAYRCT202606100008\",\"refundNo\":\"RFDRCT202606100008\",\"channelRefundNo\":\"MOCK-REFUND-RFDRCT202606100008\",\"tradeStatus\":\"REFUND_SUCCESS\",\"amountCent\":18000,\"case\":\"refund_success_callback\"}', '2026-06-10 15:18:18', '2026-06-10 15:18:18', '2026-06-10 15:18:18', '2026-06-10 15:18:18');
INSERT INTO `pay_callback_record` VALUES (13, 'MOCK', 'REFUND', 'PAYRCT202606100009', 'ORF1781078073650ED3D88', 'RCT202606100009', 'ORF1781078073650ED3D88', 'MOCK-REFUND-ORF1781078073650ED3D88', 19000, 'REFUND_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"callbackType\":\"REFUND\",\"orderNo\":\"RCT202606100009\",\"payOrderNo\":\"PAYRCT202606100009\",\"refundNo\":\"ORF1781078073650ED3D88\",\"channelRefundNo\":\"MOCK-REFUND-ORF1781078073650ED3D88\",\"tradeStatus\":\"REFUND_SUCCESS\",\"amountCent\":19000,\"case\":\"refund_success_callback\"}', '2026-06-10 15:54:47', '2026-06-10 15:54:47', '2026-06-10 15:54:47', '2026-06-10 15:54:47');
INSERT INTO `pay_callback_record` VALUES (14, 'MOCK', 'PAY', 'PAY1781081460161FDB898', NULL, 'ORD17810814566188EA786', 'PAY1781081460161FDB898', 'MOCK-TX-PAY1781081460161FDB898', 22000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'RECEIVED', '渠道成功回调晚于本地取消，待人工同步支付状态', '{\"channel\":\"MOCK\",\"callbackType\":\"PAY\",\"orderNo\":\"ORD17810814566188EA786\",\"payOrderNo\":\"PAY1781081460161FDB898\",\"transactionNo\":\"MOCK-TX-PAY1781081460161FDB898\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":22000,\"case\":\"local_closed_channel_success\"}', '2026-06-10 16:51:00', NULL, '2026-06-10 16:51:00', '2026-06-10 16:51:00');
INSERT INTO `pay_callback_record` VALUES (15, 'MOCK', 'PAY', 'PAY1781076298726911D13', NULL, 'ORD17810762227909DD3A7', 'PAY1781076298726911D13', '', 18000, 'TRADE_CLOSED', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"callbackType\":\"PAY\",\"orderNo\":\"ORD17810762227909DD3A7\",\"payOrderNo\":\"PAY1781076298726911D13\",\"tradeStatus\":\"TRADE_CLOSED\",\"amountCent\":18000,\"case\":\"local_closed_channel_closed\"}', '2026-06-10 15:27:21', '2026-06-10 15:27:21', '2026-06-10 15:27:21', '2026-06-10 15:27:21');
INSERT INTO `pay_callback_record` VALUES (16, 'MOCK', 'PAY', 'PAYRCT202606100004', NULL, 'RCT202606100004', 'PAYRCT202606100004', 'MOCK-TXN-RCT-004', 14000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '人工同步支付状态后补处理成功', '{\"channel\":\"MOCK\",\"callbackType\":\"PAY\",\"orderNo\":\"RCT202606100004\",\"payOrderNo\":\"PAYRCT202606100004\",\"transactionNo\":\"MOCK-TXN-RCT-004\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":14000,\"case\":\"manual_sync_after_callback_received\"}', '2026-06-10 15:13:49', '2026-06-10 15:13:49', '2026-06-10 15:13:49', '2026-06-10 15:13:49');
INSERT INTO `pay_callback_record` VALUES (17, 'MOCK', 'PAY', 'PAY17812006930058A5FF0', NULL, 'ORD17812006903496B061E', 'PAY17812006930058A5FF0', 'MOCK-ORD17812006903496B061E', 21000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD17812006903496B061E\",\"payOrderNo\":\"PAY17812006930058A5FF0\",\"transactionNo\":\"MOCK-ORD17812006903496B061E\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":21000}', '2026-06-12 01:58:14', '2026-06-12 01:58:14', '2026-06-12 01:58:14', '2026-06-12 01:58:14');
INSERT INTO `pay_callback_record` VALUES (18, 'MOCK', 'PAY', 'PAY17815199015757B45BD', NULL, 'ORD178151990140736FC43', 'PAY17815199015757B45BD', 'MOCK-ORD178151990140736FC43', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD178151990140736FC43\",\"payOrderNo\":\"PAY17815199015757B45BD\",\"transactionNo\":\"MOCK-ORD178151990140736FC43\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-15 18:38:22', '2026-06-15 18:38:22', '2026-06-15 18:38:22', '2026-06-15 18:38:22');
INSERT INTO `pay_callback_record` VALUES (19, 'MOCK', 'PAY', 'PAY17815228605865C60C0', NULL, 'ORD17815228604692C7570', 'PAY17815228605865C60C0', 'MOCK-ORD17815228604692C7570', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD17815228604692C7570\",\"payOrderNo\":\"PAY17815228605865C60C0\",\"transactionNo\":\"MOCK-ORD17815228604692C7570\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-15 19:27:41', '2026-06-15 19:27:41', '2026-06-15 19:27:41', '2026-06-15 19:27:41');
INSERT INTO `pay_callback_record` VALUES (20, 'MOCK', 'PAY', 'PAY1781523813708BDCDE3', NULL, 'ORD178152381362132B276', 'PAY1781523813708BDCDE3', 'MOCK-ORD178152381362132B276', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD178152381362132B276\",\"payOrderNo\":\"PAY1781523813708BDCDE3\",\"transactionNo\":\"MOCK-ORD178152381362132B276\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-15 19:43:34', '2026-06-15 19:43:34', '2026-06-15 19:43:34', '2026-06-15 19:43:34');
INSERT INTO `pay_callback_record` VALUES (21, 'MOCK', 'PAY', 'PAY1781523860491644E8B', NULL, 'ORD17815238604099B7C62', 'PAY1781523860491644E8B', 'MOCK-ORD17815238604099B7C62', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD17815238604099B7C62\",\"payOrderNo\":\"PAY1781523860491644E8B\",\"transactionNo\":\"MOCK-ORD17815238604099B7C62\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-15 19:44:21', '2026-06-15 19:44:21', '2026-06-15 19:44:21', '2026-06-15 19:44:21');
INSERT INTO `pay_callback_record` VALUES (22, 'MOCK', 'PAY', 'PAY17815239408871F527F', NULL, 'ORD1781523940797C6EC98', 'PAY17815239408871F527F', 'MOCK-ORD1781523940797C6EC98', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD1781523940797C6EC98\",\"payOrderNo\":\"PAY17815239408871F527F\",\"transactionNo\":\"MOCK-ORD1781523940797C6EC98\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-15 19:45:41', '2026-06-15 19:45:41', '2026-06-15 19:45:41', '2026-06-15 19:45:41');
INSERT INTO `pay_callback_record` VALUES (23, 'MOCK', 'PAY', 'PAY17815243299209299BD', NULL, 'ORD1781524329822DF7ACD', 'PAY17815243299209299BD', 'MOCK-ORD1781524329822DF7ACD', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD1781524329822DF7ACD\",\"payOrderNo\":\"PAY17815243299209299BD\",\"transactionNo\":\"MOCK-ORD1781524329822DF7ACD\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-15 19:52:10', '2026-06-15 19:52:10', '2026-06-15 19:52:10', '2026-06-15 19:52:10');
INSERT INTO `pay_callback_record` VALUES (24, 'MOCK', 'PAY', 'PAY1781524396891F664A3', NULL, 'ORD178152439679881F09C', 'PAY1781524396891F664A3', 'MOCK-ORD178152439679881F09C', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD178152439679881F09C\",\"payOrderNo\":\"PAY1781524396891F664A3\",\"transactionNo\":\"MOCK-ORD178152439679881F09C\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-15 19:53:17', '2026-06-15 19:53:17', '2026-06-15 19:53:17', '2026-06-15 19:53:17');
INSERT INTO `pay_callback_record` VALUES (25, 'MOCK', 'PAY', 'PAY1781524422877FE00BF', NULL, 'ORD1781524417974BA4CFF', 'PAY1781524422877FE00BF', 'MOCK-ORD1781524417974BA4CFF', 57000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD1781524417974BA4CFF\",\"payOrderNo\":\"PAY1781524422877FE00BF\",\"transactionNo\":\"MOCK-ORD1781524417974BA4CFF\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":57000}', '2026-06-15 19:53:44', '2026-06-15 19:53:44', '2026-06-15 19:53:44', '2026-06-15 19:53:44');
INSERT INTO `pay_callback_record` VALUES (26, 'MOCK', 'PAY', 'PAY1781526648667E02708', NULL, 'ORD1781526648573A839F8', 'PAY1781526648667E02708', 'MOCK-ORD1781526648573A839F8', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD1781526648573A839F8\",\"payOrderNo\":\"PAY1781526648667E02708\",\"transactionNo\":\"MOCK-ORD1781526648573A839F8\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-15 20:30:49', '2026-06-15 20:30:49', '2026-06-15 20:30:49', '2026-06-15 20:30:49');
INSERT INTO `pay_callback_record` VALUES (27, 'MOCK', 'PAY', 'PAY17815267227274444CD', NULL, 'ORD178152672263850B15C', 'PAY17815267227274444CD', 'MOCK-ORD178152672263850B15C', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD178152672263850B15C\",\"payOrderNo\":\"PAY17815267227274444CD\",\"transactionNo\":\"MOCK-ORD178152672263850B15C\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-15 20:32:03', '2026-06-15 20:32:03', '2026-06-15 20:32:03', '2026-06-15 20:32:03');
INSERT INTO `pay_callback_record` VALUES (28, 'MOCK', 'PAY', 'PAY178152809776537175B', NULL, 'ORD178152809766513D1B9', 'PAY178152809776537175B', 'MOCK-ORD178152809766513D1B9', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD178152809766513D1B9\",\"payOrderNo\":\"PAY178152809776537175B\",\"transactionNo\":\"MOCK-ORD178152809766513D1B9\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-15 20:54:58', '2026-06-15 20:54:58', '2026-06-15 20:54:58', '2026-06-15 20:54:58');
INSERT INTO `pay_callback_record` VALUES (29, 'MOCK', 'PAY', 'PAY1781528713480A934D9', NULL, 'ORD1781528713367F731BD', 'PAY1781528713480A934D9', 'MOCK-ORD1781528713367F731BD', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD1781528713367F731BD\",\"payOrderNo\":\"PAY1781528713480A934D9\",\"transactionNo\":\"MOCK-ORD1781528713367F731BD\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-15 21:05:13', '2026-06-15 21:05:14', '2026-06-15 21:05:13', '2026-06-15 21:05:14');
INSERT INTO `pay_callback_record` VALUES (30, 'MOCK', 'PAY', 'PAY1781528713480A934D9', NULL, 'ORD1781528713367F731BD', 'PAY1781528713480A934D9', 'MOCK-ORD1781528713367F731BD', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'IGNORED_DUPLICATE', '支付单已成功', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD1781528713367F731BD\",\"payOrderNo\":\"PAY1781528713480A934D9\",\"transactionNo\":\"MOCK-ORD1781528713367F731BD\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-15 21:05:14', '2026-06-15 21:05:14', '2026-06-15 21:05:14', '2026-06-15 21:05:14');
INSERT INTO `pay_callback_record` VALUES (31, 'MOCK', 'PAY', 'PAY1781528713480A934D9', NULL, 'ORD1781528713367F731BD', 'PAY1781528713480A934D9', 'MOCK-ORD1781528713367F731BD', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'IGNORED_DUPLICATE', '支付单已成功', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD1781528713367F731BD\",\"payOrderNo\":\"PAY1781528713480A934D9\",\"transactionNo\":\"MOCK-ORD1781528713367F731BD\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-15 21:05:14', '2026-06-15 21:05:14', '2026-06-15 21:05:14', '2026-06-15 21:05:14');
INSERT INTO `pay_callback_record` VALUES (32, 'MOCK', 'PAY', 'PAY178154211823272684B', NULL, 'ORD17815421181506A2B36', 'PAY178154211823272684B', 'MOCK-ORD17815421181506A2B36', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD17815421181506A2B36\",\"payOrderNo\":\"PAY178154211823272684B\",\"transactionNo\":\"MOCK-ORD17815421181506A2B36\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-16 00:48:38', '2026-06-16 00:48:38', '2026-06-16 00:48:38', '2026-06-16 00:48:38');
INSERT INTO `pay_callback_record` VALUES (33, 'MOCK', 'PAY', 'PAY17815425522336B688D', NULL, 'ORD17815425521398BF4BA', 'PAY17815425522336B688D', 'MOCK-ORD17815425521398BF4BA', 10000, 'TRADE_SUCCESS', 'MOCK-SIGN', 1, 'PROCESSED', '', '{\"channel\":\"MOCK\",\"orderNo\":\"ORD17815425521398BF4BA\",\"payOrderNo\":\"PAY17815425522336B688D\",\"transactionNo\":\"MOCK-ORD17815425521398BF4BA\",\"tradeStatus\":\"TRADE_SUCCESS\",\"amountCent\":10000}', '2026-06-16 00:55:52', '2026-06-16 00:55:52', '2026-06-16 00:55:52', '2026-06-16 00:55:52');

-- ----------------------------
-- Table structure for pay_order
-- ----------------------------
DROP TABLE IF EXISTS `pay_order`;
CREATE TABLE `pay_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '支付单ID',
  `pay_order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付单号',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务订单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `pay_amount_cent` bigint NOT NULL COMMENT '支付金额分',
  `pay_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付状态',
  `pay_channel` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MOCK' COMMENT '支付渠道',
  `transaction_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '三方流水号',
  `callback_payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '回调报文',
  `notify_time` datetime NULL DEFAULT NULL COMMENT '通知时间',
  `paid_at` datetime NULL DEFAULT NULL COMMENT '支付完成时间',
  `idempotent_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '幂等键',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pay_order_pay_order_no`(`pay_order_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_pay_order_idempotent_key`(`idempotent_key` ASC) USING BTREE,
  INDEX `idx_pay_order_status_id`(`pay_status` ASC, `id` ASC) USING BTREE,
  INDEX `idx_pay_order_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_pay_order_pay_order_no`(`pay_order_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pay_order
-- ----------------------------
INSERT INTO `pay_order` VALUES (1, 'PAYRCT202606100001', 'RCT202606100001', 1, 10000, 'SUCCESS', 'MOCK', 'MOCK-TXN-RCT-001', '{\"channelStatus\":\"SUCCESS\"}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', 'PAY:RCT:001', 0, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `pay_order` VALUES (2, 'PAYRCT202606100002', 'RCT202606100002', 1, 12000, 'CLOSED', 'MOCK', '', '{\"closedByOrderStatus\":\"TIMEOUT_CANCELLED\"}', NULL, NULL, 'PAY:RCT:002', 1, '2026-06-10 10:00:00', '2026-06-10 15:13:53', NULL);
INSERT INTO `pay_order` VALUES (3, 'PAYRCT202606100003', 'RCT202606100003', 1, 13000, 'SUCCESS', 'MOCK', 'MOCK-TXN-RCT-003', '{\"channelStatus\":\"SUCCESS\"}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', 'PAY:RCT:003', 0, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `pay_order` VALUES (4, 'PAYRCT202606100004', 'RCT202606100004', 1, 14000, 'CLOSED', 'MOCK', '', '{\"closedByOrderStatus\":\"TIMEOUT_CANCELLED\"}', NULL, NULL, 'PAY:RCT:004', 1, '2026-06-10 10:00:00', '2026-06-10 15:13:53', NULL);
INSERT INTO `pay_order` VALUES (5, 'PAYRCT202606100005', 'RCT202606100005', 1, 15000, 'SUCCESS', 'MOCK', 'MOCK-TXN-RCT-005', '{\"channelStatus\":\"MISSING\"}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', 'PAY:RCT:005', 0, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `pay_order` VALUES (6, 'PAYRCT202606100007', 'RCT202606100007', 1, 17000, 'SUCCESS', 'MOCK', 'MOCK-TXN-RCT-007', '{\"channelStatus\":\"SUCCESS\",\"amountCent\":17000}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', 'PAY:RCT:007', 0, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `pay_order` VALUES (7, 'PAYRCT202606100008', 'RCT202606100008', 1, 18000, 'REFUNDED', 'MOCK', 'MOCK-TXN-RCT-008', '{\"refundSuccessAt\":\"2026-06-10T15:18:17.783226800\",\"reason\":\"refund_sync\",\"refundAmountCent\":18000}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', 'PAY:RCT:008', 1, '2026-06-10 10:00:00', '2026-06-10 15:18:17', NULL);
INSERT INTO `pay_order` VALUES (8, 'PAYRCT202606100009', 'RCT202606100009', 1, 19000, 'REFUNDED', 'MOCK', 'MOCK-TXN-RCT-009', '{\"refundSuccessAt\":\"2026-06-10T15:54:47.294308100\",\"reason\":\"refund_sync\",\"refundAmountCent\":19000}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', 'PAY:RCT:009', 3, '2026-06-10 10:00:00', '2026-06-10 15:54:47', NULL);
INSERT INTO `pay_order` VALUES (9, 'PAYRCT202606100010', 'RCT202606100010', 1, 20000, 'SUCCESS', 'MOCK', 'MOCK-TXN-RCT-010', '{\"channelStatus\":\"SUCCESS\"}', '2026-06-10 10:00:00', '2026-06-10 10:00:00', 'PAY:RCT:010', 0, '2026-06-10 10:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `pay_order` VALUES (10, 'PAY1781075978484B35E89', 'ORD1781075976142E01526', 1, 21000, 'SUCCESS', 'MOCK', 'MOCK-ORD1781075976142E01526', '{\"orderNo\":\"ORD1781075976142E01526\",\"channel\":\"MOCK\",\"successTime\":\"2026-06-10T15:19:39.682889600\"}', NULL, NULL, 'PAY:MOCK:ORD1781075976142E01526', 2, '2026-06-10 15:19:38', '2026-06-10 15:19:40', NULL);
INSERT INTO `pay_order` VALUES (11, 'PAY1781076004337B352E6', 'ORD1781076001361016574', 1, 21000, 'SUCCESS', 'MOCK', 'MOCK-ORD1781076001361016574', '{\"orderNo\":\"ORD1781076001361016574\",\"channel\":\"MOCK\",\"successTime\":\"2026-06-10T15:20:05.868279\"}', NULL, NULL, 'PAY:MOCK:ORD1781076001361016574', 2, '2026-06-10 15:20:03', '2026-06-10 15:20:05', NULL);
INSERT INTO `pay_order` VALUES (12, 'PAY1781076298726911D13', 'ORD17810762227909DD3A7', 1, 18000, 'CLOSED', 'MOCK', '', '{\"closedByOrderStatus\":\"CANCELLED\"}', NULL, NULL, 'PAY:MOCK:ORD17810762227909DD3A7', 2, '2026-06-10 15:24:58', '2026-06-10 15:27:21', NULL);
INSERT INTO `pay_order` VALUES (13, 'PAY1781081460161FDB898', 'ORD17810814566188EA786', 1, 22000, 'CLOSED', 'MOCK', '', '{\"closedByOrderStatus\":\"CANCELLED\"}', NULL, NULL, 'PAY:MOCK:ORD17810814566188EA786', 2, '2026-06-10 16:51:00', '2026-06-10 16:51:48', NULL);
INSERT INTO `pay_order` VALUES (14, 'PAY17812006930058A5FF0', 'ORD17812006903496B061E', 1, 21000, 'REFUNDING', 'MOCK', 'MOCK-ORD17812006903496B061E', '{\"refundingAt\":\"2026-06-12T01:58:31.162654\",\"reason\":\"...\"}', NULL, NULL, 'PAY:MOCK:ORD17812006903496B061E', 4, '2026-06-12 01:58:13', '2026-06-12 01:58:31', NULL);
INSERT INTO `pay_order` VALUES (15, 'PAY17815199015757B45BD', 'ORD178151990140736FC43', 1, 10000, 'SUCCESS', 'MOCK', 'MOCK-ORD178151990140736FC43', '{\"orderNo\":\"ORD178151990140736FC43\",\"channel\":\"MOCK\",\"successTime\":\"2026-06-15T18:38:21.786246100\"}', NULL, NULL, 'PAY:MOCK:ORD178151990140736FC43', 2, '2026-06-15 18:38:21', '2026-06-15 18:38:21', NULL);
INSERT INTO `pay_order` VALUES (16, 'PAY17815228605865C60C0', 'ORD17815228604692C7570', 1, 10000, 'SUCCESS', 'MOCK', 'MOCK-ORD17815228604692C7570', '{\"orderNo\":\"ORD17815228604692C7570\",\"channel\":\"MOCK\",\"successTime\":\"2026-06-15T19:27:40.664791900\"}', NULL, NULL, 'PAY:MOCK:ORD17815228604692C7570', 2, '2026-06-15 19:27:40', '2026-06-15 19:27:40', NULL);
INSERT INTO `pay_order` VALUES (17, 'PAY1781523813708BDCDE3', 'ORD178152381362132B276', 1, 10000, 'SUCCESS', 'MOCK', 'MOCK-ORD178152381362132B276', '{\"orderNo\":\"ORD178152381362132B276\",\"channel\":\"MOCK\",\"successTime\":\"2026-06-15T19:43:33.770203200\"}', NULL, NULL, 'PAY:MOCK:ORD178152381362132B276', 2, '2026-06-15 19:43:33', '2026-06-15 19:43:33', NULL);
INSERT INTO `pay_order` VALUES (18, 'PAY1781523860491644E8B', 'ORD17815238604099B7C62', 1, 10000, 'SUCCESS', 'MOCK', 'MOCK-ORD17815238604099B7C62', '{\"orderNo\":\"ORD17815238604099B7C62\",\"channel\":\"MOCK\",\"successTime\":\"2026-06-15T19:44:20.544067\"}', NULL, NULL, 'PAY:MOCK:ORD17815238604099B7C62', 2, '2026-06-15 19:44:20', '2026-06-15 19:44:20', NULL);
INSERT INTO `pay_order` VALUES (19, 'PAY17815239408871F527F', 'ORD1781523940797C6EC98', 1, 10000, 'SUCCESS', 'MOCK', 'MOCK-ORD1781523940797C6EC98', '{\"orderNo\":\"ORD1781523940797C6EC98\",\"channel\":\"MOCK\",\"successTime\":\"2026-06-15T19:45:40.943535\"}', NULL, NULL, 'PAY:MOCK:ORD1781523940797C6EC98', 2, '2026-06-15 19:45:40', '2026-06-15 19:45:40', NULL);
INSERT INTO `pay_order` VALUES (20, 'PAY17815243299209299BD', 'ORD1781524329822DF7ACD', 1, 10000, 'SUCCESS', 'MOCK', 'MOCK-ORD1781524329822DF7ACD', '{\"orderNo\":\"ORD1781524329822DF7ACD\",\"channel\":\"MOCK\",\"successTime\":\"2026-06-15T19:52:10.024114700\"}', NULL, NULL, 'PAY:MOCK:ORD1781524329822DF7ACD', 2, '2026-06-15 19:52:09', '2026-06-15 19:52:10', NULL);
INSERT INTO `pay_order` VALUES (21, 'PAY1781524396891F664A3', 'ORD178152439679881F09C', 1, 10000, 'SUCCESS', 'MOCK', 'MOCK-ORD178152439679881F09C', '{\"orderNo\":\"ORD178152439679881F09C\",\"channel\":\"MOCK\",\"successTime\":\"2026-06-15T19:53:16.950829600\"}', NULL, NULL, 'PAY:MOCK:ORD178152439679881F09C', 2, '2026-06-15 19:53:16', '2026-06-15 19:53:16', NULL);
INSERT INTO `pay_order` VALUES (22, 'PAY17815244206967DDC12', 'ORD1781524417974BA4CFF', 1, 57000, 'CLOSED', 'ALIPAY_PC', '', '{\"closedBy\":\"switch_channel_to_MOCK\"}', NULL, NULL, 'PAY:ALIPAY_PC:ORD1781524417974BA4CFF', 1, '2026-06-15 19:53:40', '2026-06-15 19:53:42', NULL);
INSERT INTO `pay_order` VALUES (23, 'PAY1781524422877FE00BF', 'ORD1781524417974BA4CFF', 1, 57000, 'SUCCESS', 'MOCK', 'MOCK-ORD1781524417974BA4CFF', '{\"orderNo\":\"ORD1781524417974BA4CFF\",\"channel\":\"MOCK\",\"successTime\":\"2026-06-15T19:53:43.741809200\"}', NULL, NULL, 'PAY:MOCK:ORD1781524417974BA4CFF', 2, '2026-06-15 19:53:42', '2026-06-15 19:53:43', NULL);
INSERT INTO `pay_order` VALUES (24, 'PAY1781526648667E02708', 'ORD1781526648573A839F8', 1, 10000, 'REFUNDING', 'MOCK', 'MOCK-ORD1781526648573A839F8', '{\"refundingAt\":\"2026-06-15T20:30:48.860166\",\"reason\":\"商品与描述不符\"}', NULL, NULL, 'PAY:MOCK:ORD1781526648573A839F8', 4, '2026-06-15 20:30:48', '2026-06-15 20:30:48', NULL);
INSERT INTO `pay_order` VALUES (25, 'PAY17815267227274444CD', 'ORD178152672263850B15C', 1, 10000, 'REFUNDING', 'MOCK', 'MOCK-ORD178152672263850B15C', '{\"refundingAt\":\"2026-06-15T20:32:02.866914800\",\"reason\":\"商品与描述不符\"}', NULL, NULL, 'PAY:MOCK:ORD178152672263850B15C', 4, '2026-06-15 20:32:02', '2026-06-15 20:32:02', NULL);
INSERT INTO `pay_order` VALUES (26, 'PAY178152809776537175B', 'ORD178152809766513D1B9', 1, 10000, 'REFUNDING', 'MOCK', 'MOCK-ORD178152809766513D1B9', '{\"refundingAt\":\"2026-06-15T20:54:58.640731700\",\"reason\":\"P4 refund\"}', NULL, NULL, 'PAY:MOCK:ORD178152809766513D1B9', 4, '2026-06-15 20:54:57', '2026-06-15 20:54:58', NULL);
INSERT INTO `pay_order` VALUES (27, 'PAY1781528713480A934D9', 'ORD1781528713367F731BD', 1, 10000, 'REFUNDING', 'MOCK', 'MOCK-ORD1781528713367F731BD', '{\"refundingAt\":\"2026-06-15T21:05:13.741517800\",\"reason\":\"商品与描述不符\"}', NULL, NULL, 'PAY:MOCK:ORD1781528713367F731BD', 4, '2026-06-15 21:05:13', '2026-06-15 21:05:13', NULL);
INSERT INTO `pay_order` VALUES (28, 'PAY178154211823272684B', 'ORD17815421181506A2B36', 1, 10000, 'SUCCESS', 'MOCK', 'MOCK-ORD17815421181506A2B36', '{\"orderNo\":\"ORD17815421181506A2B36\",\"channel\":\"MOCK\",\"successTime\":\"2026-06-16T00:48:38.308648200\"}', NULL, NULL, 'PAY:MOCK:ORD17815421181506A2B36', 2, '2026-06-16 00:48:38', '2026-06-16 00:48:38', NULL);
INSERT INTO `pay_order` VALUES (29, 'PAY17815425522336B688D', 'ORD17815425521398BF4BA', 1, 10000, 'SUCCESS', 'MOCK', 'MOCK-ORD17815425521398BF4BA', '{\"orderNo\":\"ORD17815425521398BF4BA\",\"channel\":\"MOCK\",\"successTime\":\"2026-06-16T00:55:52.303980900\"}', NULL, NULL, 'PAY:MOCK:ORD17815425521398BF4BA', 2, '2026-06-16 00:55:52', '2026-06-16 00:55:52', NULL);

-- ----------------------------
-- Table structure for pay_reconcile_channel_bill_item
-- ----------------------------
DROP TABLE IF EXISTS `pay_reconcile_channel_bill_item`;
CREATE TABLE `pay_reconcile_channel_bill_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `biz_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `out_trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `pay_order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `refund_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `channel_trade_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `channel_refund_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `channel_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `amount_cent` bigint NULL DEFAULT NULL,
  `fee_cent` bigint NOT NULL DEFAULT 0,
  `trade_time` datetime NULL DEFAULT NULL,
  `raw_line` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_biz`(`task_id` ASC, `biz_type` ASC) USING BTREE,
  INDEX `idx_out_trade_no`(`out_trade_no` ASC) USING BTREE,
  INDEX `idx_refund_no`(`refund_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pay_reconcile_channel_bill_item
-- ----------------------------
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (1, 2, 'PAY', 'MOCK', 'MOCK-LONG-5', 'ORD-MOCK-LONG-5', 'MOCK-LONG-5', NULL, 'MOCK-TX-LONG-5', NULL, 'TRADE_SUCCESS', 9900, 0, '2026-06-11 15:49:36', '{\"mockDiff\":\"第1条：长款，渠道有账本地无账\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (2, 2, 'REFUND', 'MOCK', 'PAYRCT202606100008', 'RCT202606100008', 'PAYRCT202606100008', 'RFDRCT202606100008', 'MOCK-TX-PAYRCT202606100008', 'MOCK-REFUND-RFDRCT202606100008', 'REFUND_SUCCESS', 18000, 0, '2026-06-10 15:18:18', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"正常一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (3, 2, 'PAY', 'MOCK', 'PAY1781081460161FDB898', 'ORD17810814566188EA786', 'PAY1781081460161FDB898', NULL, 'MOCK-TX-PAY1781081460161FDB898', NULL, 'TRADE_SUCCESS', 22000, 0, '2026-06-10 16:51:00', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"第4条：状态不一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (4, 2, 'PAY', 'MOCK', 'PAY1781076298726911D13', 'ORD17810762227909DD3A7', 'PAY1781076298726911D13', NULL, 'MOCK-TX-PAY1781076298726911D13', NULL, 'TRADE_CLOSED', 18000, 0, '2026-06-10 15:24:58', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"正常一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (5, 2, 'PAY', 'MOCK', 'PAY1781076004337B352E6', 'ORD1781076001361016574', 'PAY1781076004337B352E6', NULL, 'MOCK-TX-PAY1781076004337B352E6', NULL, 'TRADE_SUCCESS', 21000, 0, '2026-06-10 15:20:03', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"正常一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (6, 2, 'PAY', 'MOCK', 'PAY1781075978484B35E89', 'ORD1781075976142E01526', 'PAY1781075978484B35E89', NULL, 'MOCK-TX-PAY1781075978484B35E89', NULL, 'TRADE_SUCCESS', 21000, 0, '2026-06-10 15:19:38', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"正常一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (7, 2, 'PAY', 'MOCK', 'PAYRCT202606100010', 'RCT202606100010', 'PAYRCT202606100010', NULL, 'MOCK-TX-PAYRCT202606100010', NULL, 'TRADE_SUCCESS', 20000, 0, '2026-06-10 10:00:00', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"正常一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (8, 2, 'PAY', 'MOCK', 'PAYRCT202606100009', 'RCT202606100009', 'PAYRCT202606100009', NULL, 'MOCK-TX-PAYRCT202606100009', NULL, 'TRADE_SUCCESS', 19000, 0, '2026-06-10 10:00:00', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"正常一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (9, 2, 'PAY', 'MOCK', 'PAYRCT202606100008', 'RCT202606100008', 'PAYRCT202606100008', NULL, 'MOCK-TX-PAYRCT202606100008', NULL, 'TRADE_SUCCESS', 18000, 0, '2026-06-10 10:00:00', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"正常一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (10, 2, 'PAY', 'MOCK', 'PAYRCT202606100007', 'RCT202606100007', 'PAYRCT202606100007', NULL, 'MOCK-TX-PAYRCT202606100007', NULL, 'TRADE_SUCCESS', 17000, 0, '2026-06-10 10:00:00', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"正常一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (11, 2, 'PAY', 'MOCK', 'PAYRCT202606100005', 'RCT202606100005', 'PAYRCT202606100005', NULL, 'MOCK-TX-PAYRCT202606100005', NULL, 'TRADE_SUCCESS', 15000, 0, '2026-06-10 10:00:00', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"正常一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (12, 2, 'PAY', 'MOCK', 'PAYRCT202606100004', 'RCT202606100004', 'PAYRCT202606100004', NULL, 'MOCK-TX-PAYRCT202606100004', NULL, 'TRADE_CLOSED', 14000, 0, '2026-06-10 10:00:00', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"正常一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (13, 2, 'PAY', 'MOCK', 'PAYRCT202606100003', 'RCT202606100003', 'PAYRCT202606100003', NULL, 'MOCK-TX-PAYRCT202606100003', NULL, 'TRADE_SUCCESS', 13000, 0, '2026-06-10 10:00:00', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"正常一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (14, 2, 'PAY', 'MOCK', 'PAYRCT202606100002', 'RCT202606100002', 'PAYRCT202606100002', NULL, 'MOCK-TX-PAYRCT202606100002', NULL, 'TRADE_CLOSED', 12000, 0, '2026-06-10 10:00:00', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"正常一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (15, 2, 'PAY', 'MOCK', 'PAYRCT202606100001', 'RCT202606100001', 'PAYRCT202606100001', NULL, 'MOCK-TX-PAYRCT202606100001', NULL, 'TRADE_SUCCESS', 10000, 0, '2026-06-10 10:00:00', '{\"mockMode\":\"FULL_TEST\",\"mockDiff\":\"正常一致\"}', '2026-06-11 15:49:36');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (16, 4, 'PAY', 'MOCK', 'PAY1781523813708BDCDE3', 'ORD178152381362132B276', 'PAY1781523813708BDCDE3', NULL, 'MOCK-TX-PAY1781523813708BDCDE3', NULL, 'TRADE_SUCCESS', 10000, 0, '2026-06-15 19:43:33', '{\"mockMode\":\"NORMAL\",\"mockDiff\":\"正常一致\"}', '2026-06-15 19:43:34');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (17, 4, 'PAY', 'MOCK', 'PAY17815228605865C60C0', 'ORD17815228604692C7570', 'PAY17815228605865C60C0', NULL, 'MOCK-TX-PAY17815228605865C60C0', NULL, 'TRADE_SUCCESS', 10000, 0, '2026-06-15 19:27:40', '{\"mockMode\":\"NORMAL\",\"mockDiff\":\"正常一致\"}', '2026-06-15 19:43:34');
INSERT INTO `pay_reconcile_channel_bill_item` VALUES (18, 4, 'PAY', 'MOCK', 'PAY17815199015757B45BD', 'ORD178151990140736FC43', 'PAY17815199015757B45BD', NULL, 'MOCK-TX-PAY17815199015757B45BD', NULL, 'TRADE_SUCCESS', 10000, 0, '2026-06-15 18:38:21', '{\"mockMode\":\"NORMAL\",\"mockDiff\":\"正常一致\"}', '2026-06-15 19:43:34');

-- ----------------------------
-- Table structure for pay_reconcile_diff_item
-- ----------------------------
DROP TABLE IF EXISTS `pay_reconcile_diff_item`;
CREATE TABLE `pay_reconcile_diff_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `biz_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `diff_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `diff_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `pay_order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `refund_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `local_item_id` bigint NULL DEFAULT NULL,
  `channel_item_id` bigint NULL DEFAULT NULL,
  `local_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `channel_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `local_amount_cent` bigint NULL DEFAULT NULL,
  `channel_amount_cent` bigint NULL DEFAULT NULL,
  `diff_amount_cent` bigint NULL DEFAULT NULL,
  `suggested_action` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `process_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING',
  `process_remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `processed_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `processed_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_status`(`task_id` ASC, `process_status` ASC) USING BTREE,
  INDEX `idx_task_diff`(`task_id` ASC, `diff_type` ASC) USING BTREE,
  INDEX `idx_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_pay_order_no`(`pay_order_no` ASC) USING BTREE,
  INDEX `idx_refund_no`(`refund_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pay_reconcile_diff_item
-- ----------------------------
INSERT INTO `pay_reconcile_diff_item` VALUES (1, 2, 'REFUND', 'LOCAL_EXISTS_CHANNEL_MISSING', 'HIGH', 'RCT202606100009', 'PAYRCT202606100009', 'ORF1781078073650ED3D88', 88, NULL, 'REFUND_SUCCESS', NULL, 19000, NULL, NULL, 'CLOSE_ORDER_VOID', 'DONE', '人工确认已处理，说明=挂账跟进页确认完结闭环', 'admin', '2026-06-14 21:08:36', '2026-06-13 19:57:05', '2026-06-14 21:08:36');
INSERT INTO `pay_reconcile_diff_item` VALUES (2, 2, 'REFUND', 'MATCHED', 'LOW', 'RCT202606100008', 'PAYRCT202606100008', 'RFDRCT202606100008', 87, 46, 'REFUND_SUCCESS', 'REFUND_SUCCESS', 18000, 18000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-13 19:57:05', '2026-06-13 19:57:05');
INSERT INTO `pay_reconcile_diff_item` VALUES (3, 2, 'PAY', 'STATUS_MISMATCH', 'MEDIUM', 'ORD17810814566188EA786', 'PAY1781081460161FDB898', NULL, 84, 47, 'CLOSED', 'TRADE_SUCCESS', 22000, 22000, 0, 'SUBMIT_FINANCE_ADJUSTMENT', 'DONE', '人工确认已处理，说明=挂账跟进页人工确认完结闭环', 'admin', '2026-06-14 21:57:07', '2026-06-13 19:57:05', '2026-06-14 21:57:07');
INSERT INTO `pay_reconcile_diff_item` VALUES (4, 2, 'PAY', 'MATCHED', 'LOW', 'ORD17810762227909DD3A7', 'PAY1781076298726911D13', NULL, 83, 48, 'CLOSED', 'TRADE_CLOSED', 18000, 18000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-13 19:57:05', '2026-06-13 19:57:05');
INSERT INTO `pay_reconcile_diff_item` VALUES (5, 2, 'PAY', 'MATCHED', 'LOW', 'ORD1781076001361016574', 'PAY1781076004337B352E6', NULL, 82, 49, 'SUCCESS', 'TRADE_SUCCESS', 21000, 21000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-13 19:57:05', '2026-06-13 19:57:05');
INSERT INTO `pay_reconcile_diff_item` VALUES (6, 2, 'PAY', 'MATCHED', 'LOW', 'ORD1781075976142E01526', 'PAY1781075978484B35E89', NULL, 81, 50, 'SUCCESS', 'TRADE_SUCCESS', 21000, 21000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-13 19:57:05', '2026-06-13 19:57:05');
INSERT INTO `pay_reconcile_diff_item` VALUES (7, 2, 'PAY', 'MATCHED', 'LOW', 'RCT202606100010', 'PAYRCT202606100010', NULL, 80, 51, 'SUCCESS', 'TRADE_SUCCESS', 20000, 20000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-13 19:57:05', '2026-06-13 19:57:05');
INSERT INTO `pay_reconcile_diff_item` VALUES (8, 2, 'PAY', 'STATUS_MISMATCH', 'MEDIUM', 'RCT202606100009', 'PAYRCT202606100009', NULL, 79, 52, 'REFUNDED', 'TRADE_SUCCESS', 19000, 19000, 0, 'SYNC_PAY_STATUS', 'DONE', '人工确认已处理，说明=备注：水水水水水水水', 'admin', '2026-06-14 21:57:34', '2026-06-13 19:57:05', '2026-06-14 21:57:34');
INSERT INTO `pay_reconcile_diff_item` VALUES (9, 2, 'PAY', 'STATUS_MISMATCH', 'MEDIUM', 'RCT202606100008', 'PAYRCT202606100008', NULL, 78, 53, 'REFUNDED', 'TRADE_SUCCESS', 18000, 18000, 0, 'SYNC_PAY_STATUS', 'DONE', '已同步支付状态', 'admin', '2026-06-14 21:13:52', '2026-06-13 19:57:05', '2026-06-14 21:13:52');
INSERT INTO `pay_reconcile_diff_item` VALUES (10, 2, 'PAY', 'MATCHED', 'LOW', 'RCT202606100007', 'PAYRCT202606100007', NULL, 77, 54, 'SUCCESS', 'TRADE_SUCCESS', 17000, 17000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-13 19:57:05', '2026-06-13 19:57:05');
INSERT INTO `pay_reconcile_diff_item` VALUES (11, 2, 'PAY', 'MATCHED', 'LOW', 'RCT202606100005', 'PAYRCT202606100005', NULL, 76, 55, 'SUCCESS', 'TRADE_SUCCESS', 15000, 15000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-13 19:57:05', '2026-06-13 19:57:05');
INSERT INTO `pay_reconcile_diff_item` VALUES (12, 2, 'PAY', 'MATCHED', 'LOW', 'RCT202606100004', 'PAYRCT202606100004', NULL, 75, 56, 'CLOSED', 'TRADE_CLOSED', 14000, 14000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-13 19:57:05', '2026-06-13 19:57:05');
INSERT INTO `pay_reconcile_diff_item` VALUES (13, 2, 'PAY', 'MATCHED', 'LOW', 'RCT202606100003', 'PAYRCT202606100003', NULL, 74, 57, 'SUCCESS', 'TRADE_SUCCESS', 13000, 13000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-13 19:57:05', '2026-06-13 19:57:05');
INSERT INTO `pay_reconcile_diff_item` VALUES (14, 2, 'PAY', 'MATCHED', 'LOW', 'RCT202606100002', 'PAYRCT202606100002', NULL, 73, 58, 'CLOSED', 'TRADE_CLOSED', 12000, 12000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-13 19:57:05', '2026-06-13 19:57:05');
INSERT INTO `pay_reconcile_diff_item` VALUES (15, 2, 'PAY', 'MATCHED', 'LOW', 'RCT202606100001', 'PAYRCT202606100001', NULL, 72, 59, 'SUCCESS', 'TRADE_SUCCESS', 10000, 10000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-13 19:57:05', '2026-06-13 19:57:05');
INSERT INTO `pay_reconcile_diff_item` VALUES (16, 2, 'PAY', 'CHANNEL_EXISTS_LOCAL_MISSING', 'HIGH', 'ORD-MOCK-LONG-5', 'MOCK-LONG-5', NULL, NULL, 45, NULL, 'TRADE_SUCCESS', NULL, 9900, NULL, 'MANUAL_REVIEW', 'DONE', '已标记测试流水作废，说明=系统推荐原因：真实收款未入库 / 重复回调 / 测试流水', 'admin', '2026-06-13 19:57:19', '2026-06-13 19:57:05', '2026-06-13 19:57:19');
INSERT INTO `pay_reconcile_diff_item` VALUES (17, 4, 'PAY', 'MATCHED', 'LOW', 'ORD178152381362132B276', 'PAY1781523813708BDCDE3', NULL, 92, 60, 'SUCCESS', 'TRADE_SUCCESS', 10000, 10000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-15 19:43:34', '2026-06-15 19:43:34');
INSERT INTO `pay_reconcile_diff_item` VALUES (18, 4, 'PAY', 'MATCHED', 'LOW', 'ORD17815228604692C7570', 'PAY17815228605865C60C0', NULL, 91, 61, 'SUCCESS', 'TRADE_SUCCESS', 10000, 10000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-15 19:43:34', '2026-06-15 19:43:34');
INSERT INTO `pay_reconcile_diff_item` VALUES (19, 4, 'PAY', 'MATCHED', 'LOW', 'ORD178151990140736FC43', 'PAY17815199015757B45BD', NULL, 90, 62, 'SUCCESS', 'TRADE_SUCCESS', 10000, 10000, 0, 'NONE', 'DONE', NULL, NULL, NULL, '2026-06-15 19:43:34', '2026-06-15 19:43:34');

-- ----------------------------
-- Table structure for pay_reconcile_local_bill_item
-- ----------------------------
DROP TABLE IF EXISTS `pay_reconcile_local_bill_item`;
CREATE TABLE `pay_reconcile_local_bill_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `biz_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `pay_order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `refund_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_id` bigint NULL DEFAULT NULL,
  `local_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `order_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `amount_cent` bigint NULL DEFAULT NULL,
  `channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `transaction_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `trade_time` datetime NULL DEFAULT NULL,
  `raw_snapshot` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_biz`(`task_id` ASC, `biz_type` ASC) USING BTREE,
  INDEX `idx_pay_order_no`(`pay_order_no` ASC) USING BTREE,
  INDEX `idx_refund_no`(`refund_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pay_reconcile_local_bill_item
-- ----------------------------
INSERT INTO `pay_reconcile_local_bill_item` VALUES (1, 2, 'PAY', 'RCT202606100001', 'PAYRCT202606100001', NULL, 1, 'SUCCESS', 'PAID', 10000, 'MOCK', 'MOCK-TXN-RCT-001', '2026-06-10 10:00:00', '{\"payOrderNo\":\"PAYRCT202606100001\",\"status\":\"SUCCESS\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (2, 2, 'PAY', 'RCT202606100002', 'PAYRCT202606100002', NULL, 1, 'CLOSED', 'TIMEOUT_CANCELLED', 12000, 'MOCK', '', '2026-06-10 10:00:00', '{\"payOrderNo\":\"PAYRCT202606100002\",\"status\":\"CLOSED\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (3, 2, 'PAY', 'RCT202606100003', 'PAYRCT202606100003', NULL, 1, 'SUCCESS', 'PAID', 13000, 'MOCK', 'MOCK-TXN-RCT-003', '2026-06-10 10:00:00', '{\"payOrderNo\":\"PAYRCT202606100003\",\"status\":\"SUCCESS\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (4, 2, 'PAY', 'RCT202606100004', 'PAYRCT202606100004', NULL, 1, 'CLOSED', 'TIMEOUT_CANCELLED', 14000, 'MOCK', '', '2026-06-10 10:00:00', '{\"payOrderNo\":\"PAYRCT202606100004\",\"status\":\"CLOSED\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (5, 2, 'PAY', 'RCT202606100005', 'PAYRCT202606100005', NULL, 1, 'SUCCESS', 'PAID', 15000, 'MOCK', 'MOCK-TXN-RCT-005', '2026-06-10 10:00:00', '{\"payOrderNo\":\"PAYRCT202606100005\",\"status\":\"SUCCESS\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (6, 2, 'PAY', 'RCT202606100007', 'PAYRCT202606100007', NULL, 1, 'SUCCESS', 'PAID', 17000, 'MOCK', 'MOCK-TXN-RCT-007', '2026-06-10 10:00:00', '{\"payOrderNo\":\"PAYRCT202606100007\",\"status\":\"SUCCESS\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (7, 2, 'PAY', 'RCT202606100008', 'PAYRCT202606100008', NULL, 1, 'REFUNDED', 'REFUNDED', 18000, 'MOCK', 'MOCK-TXN-RCT-008', '2026-06-10 10:00:00', '{\"payOrderNo\":\"PAYRCT202606100008\",\"status\":\"REFUNDED\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (8, 2, 'PAY', 'RCT202606100009', 'PAYRCT202606100009', NULL, 1, 'REFUNDED', 'REFUNDED', 19000, 'MOCK', 'MOCK-TXN-RCT-009', '2026-06-10 10:00:00', '{\"payOrderNo\":\"PAYRCT202606100009\",\"status\":\"REFUNDED\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (9, 2, 'PAY', 'RCT202606100010', 'PAYRCT202606100010', NULL, 1, 'SUCCESS', 'SHIPPED', 20000, 'MOCK', 'MOCK-TXN-RCT-010', '2026-06-10 10:00:00', '{\"payOrderNo\":\"PAYRCT202606100010\",\"status\":\"SUCCESS\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (10, 2, 'PAY', 'ORD1781075976142E01526', 'PAY1781075978484B35E89', NULL, 1, 'SUCCESS', 'COMPLETED', 21000, 'MOCK', 'MOCK-ORD1781075976142E01526', '2026-06-10 15:19:38', '{\"payOrderNo\":\"PAY1781075978484B35E89\",\"status\":\"SUCCESS\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (11, 2, 'PAY', 'ORD1781076001361016574', 'PAY1781076004337B352E6', NULL, 1, 'SUCCESS', 'SHIPPED', 21000, 'MOCK', 'MOCK-ORD1781076001361016574', '2026-06-10 15:20:03', '{\"payOrderNo\":\"PAY1781076004337B352E6\",\"status\":\"SUCCESS\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (12, 2, 'PAY', 'ORD17810762227909DD3A7', 'PAY1781076298726911D13', NULL, 1, 'CLOSED', 'CANCELLED', 18000, 'MOCK', '', '2026-06-10 15:24:58', '{\"payOrderNo\":\"PAY1781076298726911D13\",\"status\":\"CLOSED\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (13, 2, 'PAY', 'ORD17810814566188EA786', 'PAY1781081460161FDB898', NULL, 1, 'CLOSED', 'CANCELLED', 22000, 'MOCK', '', '2026-06-10 16:51:00', '{\"payOrderNo\":\"PAY1781081460161FDB898\",\"status\":\"CLOSED\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (14, 2, 'REFUND', 'RCT202606100008', 'PAYRCT202606100008', 'RFDRCT202606100008', 1, 'REFUND_SUCCESS', 'REFUNDED', 18000, 'MOCK', 'MOCK-TXN-RCT-008', '2026-06-10 15:18:18', '{\"refundNo\":\"RFDRCT202606100008\",\"status\":\"REFUND_SUCCESS\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (15, 2, 'REFUND', 'RCT202606100009', 'PAYRCT202606100009', 'ORF1781078073650ED3D88', 1, 'REFUND_SUCCESS', 'REFUNDED', 19000, 'MOCK', 'MOCK-TXN-RCT-009', '2026-06-10 15:54:47', '{\"refundNo\":\"ORF1781078073650ED3D88\",\"status\":\"REFUND_SUCCESS\"}', '2026-06-11 15:49:23');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (16, 4, 'PAY', 'ORD178151990140736FC43', 'PAY17815199015757B45BD', NULL, 1, 'SUCCESS', 'COMPLETED', 10000, 'MOCK', 'MOCK-ORD178151990140736FC43', '2026-06-15 18:38:21', '{\"payOrderNo\":\"PAY17815199015757B45BD\",\"status\":\"SUCCESS\"}', '2026-06-15 19:43:34');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (17, 4, 'PAY', 'ORD17815228604692C7570', 'PAY17815228605865C60C0', NULL, 1, 'SUCCESS', 'COMPLETED', 10000, 'MOCK', 'MOCK-ORD17815228604692C7570', '2026-06-15 19:27:40', '{\"payOrderNo\":\"PAY17815228605865C60C0\",\"status\":\"SUCCESS\"}', '2026-06-15 19:43:34');
INSERT INTO `pay_reconcile_local_bill_item` VALUES (18, 4, 'PAY', 'ORD178152381362132B276', 'PAY1781523813708BDCDE3', NULL, 1, 'SUCCESS', 'PAID', 10000, 'MOCK', 'MOCK-ORD178152381362132B276', '2026-06-15 19:43:33', '{\"payOrderNo\":\"PAY1781523813708BDCDE3\",\"status\":\"SUCCESS\"}', '2026-06-15 19:43:34');

-- ----------------------------
-- Table structure for pay_reconcile_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `pay_reconcile_operation_log`;
CREATE TABLE `pay_reconcile_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `diff_item_id` bigint NULL DEFAULT NULL,
  `operation_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `operation_content` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `before_snapshot` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `after_snapshot` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `operator_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_diff_item_id`(`diff_item_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pay_reconcile_operation_log
-- ----------------------------
INSERT INTO `pay_reconcile_operation_log` VALUES (1, 1, NULL, 'COMPLETE_TASK', '完成线上对账任务归档：RC20260609MOCK35392', NULL, NULL, NULL, 'admin', '后台人工确认对账完成', '2026-06-11 22:20:50');
INSERT INTO `pay_reconcile_operation_log` VALUES (2, 2, 16, 'MARK_TEST_FLOW_VOID', '线上对账差异处理：动作=MARK_TEST_FLOW_VOID，类型=PAY，差异=CHANNEL_EXISTS_LOCAL_MISSING，订单=ORD-MOCK-LONG-5，支付单=MOCK-LONG-5，退款单=-', NULL, NULL, NULL, 'admin', '系统推荐原因：真实收款未入库 / 重复回调 / 测试流水', '2026-06-13 19:57:19');
INSERT INTO `pay_reconcile_operation_log` VALUES (3, 2, 1, 'HANG', '线上对账差异处理：动作=HANG，类型=REFUND，差异=LOCAL_EXISTS_CHANNEL_MISSING，订单=RCT202606100009，支付单=PAYRCT202606100009，退款单=ORF1781078073650ED3D88', NULL, NULL, NULL, 'admin', '系统推荐原因：渠道退款延迟入账 / 渠道账单日期不一致；判定原因：渠道退款延迟入账', '2026-06-14 21:02:34');
INSERT INTO `pay_reconcile_operation_log` VALUES (4, 2, 1, 'TRANSFER_FINANCE', '挂账差异转财务调账', NULL, NULL, NULL, 'admin', '挂账跟进页转财务调账', '2026-06-14 21:03:08');
INSERT INTO `pay_reconcile_operation_log` VALUES (5, 2, 1, 'CLOSE_HANGING', '挂账差异完结闭环', NULL, NULL, NULL, 'admin', '挂账跟进页确认完结闭环', '2026-06-14 21:08:36');
INSERT INTO `pay_reconcile_operation_log` VALUES (6, 2, 2, 'MATCH', '线上对账勾兑一致：类型=REFUND，订单=RCT202606100008，支付单=PAYRCT202606100008，退款单=RFDRCT202606100008，本地金额=18000，渠道金额=18000', NULL, NULL, NULL, 'system', '本地退款单、渠道账单、售后单金额与状态一致', '2026-06-14 21:10:24');
INSERT INTO `pay_reconcile_operation_log` VALUES (7, 2, 9, 'SYNC_PAY_STATUS', '线上对账差异处理：动作=SYNC_PAY_STATUS，类型=PAY，差异=STATUS_MISMATCH，订单=RCT202606100008，支付单=PAYRCT202606100008，退款单=-', NULL, NULL, NULL, 'admin', '', '2026-06-14 21:13:52');
INSERT INTO `pay_reconcile_operation_log` VALUES (8, 2, 2, 'TRANSFER_FINANCE', '挂账差异转财务调账：已形成财务待办', NULL, NULL, NULL, 'admin', '挂账跟进页转财务调账', '2026-06-14 21:21:35');
INSERT INTO `pay_reconcile_operation_log` VALUES (9, 2, 2, 'DONE', '线上对账一致确认：类型=REFUND，订单=RCT202606100008，支付单=PAYRCT202606100008，退款单=RFDRCT202606100008', NULL, NULL, NULL, 'admin', '本地与渠道退款金额均为18000，状态均为REFUND_SUCCESS，确认闭环', '2026-06-14 21:34:06');
INSERT INTO `pay_reconcile_operation_log` VALUES (10, 2, 2, 'FOLLOW_UP', '新增挂账跟进记录', NULL, NULL, NULL, 'admin', '测试', '2026-06-14 21:46:46');
INSERT INTO `pay_reconcile_operation_log` VALUES (11, 2, 2, 'TRANSFER_FINANCE', '挂账差异转财务调账：已形成财务待办', NULL, NULL, NULL, 'admin', '挂账跟进页转财务调账', '2026-06-14 21:47:06');
INSERT INTO `pay_reconcile_operation_log` VALUES (12, 2, 2, 'CLOSE_HANGING', '挂账差异完结闭环', NULL, NULL, NULL, 'admin', '挂账跟进页人工确认完结闭环', '2026-06-14 21:51:10');
INSERT INTO `pay_reconcile_operation_log` VALUES (13, 2, 3, 'HANG', '线上对账差异处理：动作=HANG，类型=PAY，差异=STATUS_MISMATCH，订单=ORD17810814566188EA786，支付单=PAY1781081460161FDB898，退款单=-', NULL, NULL, NULL, 'admin', '备注：测试', '2026-06-14 21:56:10');
INSERT INTO `pay_reconcile_operation_log` VALUES (14, 2, 3, 'FOLLOW_UP', '新增挂账跟进记录', NULL, NULL, NULL, 'admin', '..', '2026-06-14 21:56:26');
INSERT INTO `pay_reconcile_operation_log` VALUES (15, 2, 3, 'TRANSFER_FINANCE', '挂账差异转财务调账：已形成财务待办', NULL, NULL, NULL, 'admin', '挂账跟进页标记需财务处理（仅留痕，不生成真实调账单）', '2026-06-14 21:56:29');
INSERT INTO `pay_reconcile_operation_log` VALUES (16, 2, 3, 'FOLLOW_UP', '新增挂账跟进记录', NULL, NULL, NULL, 'admin', 'xxxxxxxxxxx', '2026-06-14 21:56:42');
INSERT INTO `pay_reconcile_operation_log` VALUES (17, 2, 3, 'CLOSE_HANGING', '挂账差异完结闭环', NULL, NULL, NULL, 'admin', '挂账跟进页人工确认完结闭环', '2026-06-14 21:57:07');
INSERT INTO `pay_reconcile_operation_log` VALUES (18, 2, 8, 'MARK_DONE', '线上对账差异处理：动作=MARK_DONE，类型=PAY，差异=STATUS_MISMATCH，订单=RCT202606100009，支付单=PAYRCT202606100009，退款单=-', NULL, NULL, NULL, 'admin', '备注：水水水水水水水', '2026-06-14 21:57:34');
INSERT INTO `pay_reconcile_operation_log` VALUES (19, 2, NULL, 'COMPLETE_TASK', '完成线上对账任务归档：RC20260610MOCK59944', NULL, NULL, NULL, 'admin', '后台人工确认对账完成', '2026-06-14 21:58:06');
INSERT INTO `pay_reconcile_operation_log` VALUES (20, 4, NULL, 'COMPLETE_TASK', '完成线上对账任务归档：RC20260615MOCK14414', NULL, NULL, NULL, 'admin', 'P2 smoke complete', '2026-06-15 19:43:35');

-- ----------------------------
-- Table structure for pay_reconcile_task
-- ----------------------------
DROP TABLE IF EXISTS `pay_reconcile_task`;
CREATE TABLE `pay_reconcile_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `reconcile_date` date NOT NULL,
  `channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CREATED',
  `local_bill_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'EMPTY',
  `channel_bill_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'EMPTY',
  `match_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NOT_MATCHED',
  `local_total_count` bigint NOT NULL DEFAULT 0,
  `local_total_amount_cent` bigint NOT NULL DEFAULT 0,
  `channel_total_count` bigint NOT NULL DEFAULT 0,
  `channel_total_amount_cent` bigint NOT NULL DEFAULT 0,
  `matched_count` bigint NOT NULL DEFAULT 0,
  `diff_count` bigint NOT NULL DEFAULT 0,
  `pending_count` bigint NOT NULL DEFAULT 0,
  `done_count` bigint NOT NULL DEFAULT 0,
  `hang_count` bigint NOT NULL DEFAULT 0,
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `completed_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `matched_at` datetime NULL DEFAULT NULL,
  `completed_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `task_no`(`task_no` ASC) USING BTREE,
  INDEX `idx_reconcile_date_channel`(`reconcile_date` ASC, `channel` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pay_reconcile_task
-- ----------------------------
INSERT INTO `pay_reconcile_task` VALUES (1, 'RC20260609MOCK35392', '2026-06-09', 'MOCK', 'COMPLETED', 'EMPTY', 'EMPTY', 'NOT_MATCHED', 0, 0, 0, 0, 0, 0, 0, 0, 0, NULL, 'admin', '后台人工确认对账完成', '2026-06-10 19:25:35', '2026-06-11 22:20:50', NULL, '2026-06-11 22:20:50');
INSERT INTO `pay_reconcile_task` VALUES (2, 'RC20260610MOCK59944', '2026-06-10', 'MOCK', 'COMPLETED', 'READY', 'READY', 'MATCHED', 15, 256500, 15, 247500, 10, 6, 0, 6, 0, NULL, 'admin', '后台人工确认对账完成', '2026-06-11 15:49:19', '2026-06-14 21:58:05', '2026-06-13 19:57:05', '2026-06-14 21:58:05');
INSERT INTO `pay_reconcile_task` VALUES (3, 'RC20260614ALIPAY37880', '2026-06-14', 'ALIPAY', 'CREATED', 'EMPTY', 'EMPTY', 'NOT_MATCHED', 0, 0, 0, 0, 0, 0, 0, 0, 0, NULL, NULL, '', '2026-06-14 23:12:17', NULL, NULL, NULL);
INSERT INTO `pay_reconcile_task` VALUES (4, 'RC20260615MOCK14414', '2026-06-15', 'MOCK', 'COMPLETED', 'READY', 'READY', 'MATCHED', 3, 30000, 3, 30000, 3, 0, 0, 0, 0, NULL, 'admin', 'P2 smoke task；归档备注=P2 smoke complete', '2026-06-15 19:43:34', '2026-06-15 19:43:35', '2026-06-15 19:43:34', '2026-06-15 19:43:35');

-- ----------------------------
-- Table structure for pay_reconciliation_record
-- ----------------------------
DROP TABLE IF EXISTS `pay_reconciliation_record`;
CREATE TABLE `pay_reconciliation_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '支付对账记录ID',
  `batch_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '对账批次号',
  `biz_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务类型：PAY/REFUND',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务订单号',
  `pay_order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付单号',
  `refund_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '退款单号',
  `local_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '本地状态',
  `channel_status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '渠道状态',
  `local_amount_cent` bigint NULL DEFAULT NULL COMMENT '本地金额分',
  `channel_amount_cent` bigint NULL DEFAULT NULL COMMENT '渠道金额分',
  `consistent` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否一致',
  `diff_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NONE' COMMENT '差异类型',
  `repair_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NONE' COMMENT '修复状态：NONE/PENDING/DONE/IGNORED',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `repaired_at` datetime NULL DEFAULT NULL COMMENT '修复时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pay_reconciliation_biz_order`(`biz_type` ASC, `order_no` ASC) USING BTREE,
  INDEX `idx_pay_reconciliation_status_time`(`consistent` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_pay_reconciliation_refund_no`(`refund_no` ASC) USING BTREE,
  INDEX `idx_pay_reconciliation_pay_order_no`(`pay_order_no` ASC) USING BTREE,
  INDEX `idx_pay_reconciliation_batch_no`(`batch_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付退款轻量对账记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pay_reconciliation_record
-- ----------------------------
INSERT INTO `pay_reconciliation_record` VALUES (1, 'RCPAY20260615183821815', 'PAY', 'ORD178151990140736FC43', 'PAY17815199015757B45BD', NULL, 'SUCCESS', 'SUCCESS', 10000, 10000, 1, 'NONE', 'NONE', '批次=RCPAY20260615183821815，支付订单、业务订单与渠道状态一致', NULL, '2026-06-15 18:38:22', '2026-06-15 18:38:22');
INSERT INTO `pay_reconciliation_record` VALUES (2, 'RCPAY20260615192740689', 'PAY', 'ORD17815228604692C7570', 'PAY17815228605865C60C0', NULL, 'SUCCESS', 'SUCCESS', 10000, 10000, 1, 'NONE', 'NONE', '批次=RCPAY20260615192740689，支付订单、业务订单与渠道状态一致', NULL, '2026-06-15 19:27:41', '2026-06-15 19:27:41');
INSERT INTO `pay_reconciliation_record` VALUES (3, 'RCPAY20260615210513657', 'REFUND', 'ORD1781528713367F731BD', 'PAY1781528713480A934D9', 'ORF1781528713716FCFAFF', 'REFUNDING', 'REFUNDING', 10000, 10000, 1, 'NONE', 'NONE', '批次=RCPAY20260615210513657，退款单、支付单与业务订单退款处理中状态一致', NULL, '2026-06-15 21:05:14', '2026-06-15 21:05:14');
INSERT INTO `pay_reconciliation_record` VALUES (4, 'RCPAY20260616004838334', 'PAY', 'ORD17815421181506A2B36', 'PAY178154211823272684B', NULL, 'SUCCESS', 'SUCCESS', 10000, 10000, 1, 'NONE', 'NONE', '批次=RCPAY20260616004838334，支付订单、业务订单与渠道状态一致', NULL, '2026-06-16 00:48:38', '2026-06-16 00:48:38');
INSERT INTO `pay_reconciliation_record` VALUES (5, 'RCPAY20260616005552329', 'PAY', 'ORD17815425521398BF4BA', 'PAY17815425522336B688D', NULL, 'SUCCESS', 'SUCCESS', 10000, 10000, 1, 'NONE', 'NONE', '批次=RCPAY20260616005552329，支付订单、业务订单与渠道状态一致', NULL, '2026-06-16 00:55:52', '2026-06-16 00:55:52');

-- ----------------------------
-- Table structure for pay_refund_order
-- ----------------------------
DROP TABLE IF EXISTS `pay_refund_order`;
CREATE TABLE `pay_refund_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '支付退款单ID',
  `refund_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '本地退款单号',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务订单号',
  `pay_order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原支付单号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `pay_channel` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MOCK' COMMENT '支付渠道',
  `refund_amount_cent` bigint NOT NULL COMMENT '退款金额分',
  `refund_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '退款状态：REFUND_PENDING/REFUNDING/REFUND_SUCCESS/REFUND_FAILED',
  `transaction_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '原三方交易号',
  `channel_refund_no` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '渠道退款流水号',
  `request_payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '退款请求摘要',
  `response_payload` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '退款响应摘要',
  `fail_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '失败原因',
  `success_at` datetime NULL DEFAULT NULL COMMENT '退款成功时间',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pay_refund_order_refund_no`(`refund_no` ASC) USING BTREE,
  INDEX `idx_pay_refund_order_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_pay_refund_order_pay_order_no`(`pay_order_no` ASC) USING BTREE,
  INDEX `idx_pay_refund_order_status_id`(`refund_status` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付退款单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pay_refund_order
-- ----------------------------
INSERT INTO `pay_refund_order` VALUES (1, 'RFDRCT202606100008', 'RCT202606100008', 'PAYRCT202606100008', 1, 'MOCK', 18000, 'REFUND_SUCCESS', 'MOCK-TXN-RCT-008', 'MOCK-REFUND-RFDRCT202606100008', '{\"reason\":\"对账退款测试\"}', '{\'channel\':\'MOCK\',\'status\':\'REFUND_SUCCESS\'}', '', '2026-06-10 15:18:18', 1, '2026-06-10 10:00:00', '2026-06-10 15:18:18');
INSERT INTO `pay_refund_order` VALUES (2, 'ORF1781078073650ED3D88', 'RCT202606100009', 'PAYRCT202606100009', 1, 'MOCK', 19000, 'REFUND_SUCCESS', 'MOCK-TXN-RCT-009', 'MOCK-REFUND-ORF1781078073650ED3D88', '{\"reason\":\"...\"}', '{\'channel\':\'MOCK\',\'status\':\'REFUND_SUCCESS\'}', '', '2026-06-10 15:54:47', 2, '2026-06-10 15:54:34', '2026-06-10 15:54:47');
INSERT INTO `pay_refund_order` VALUES (3, 'ORF17812007111280C0DE2', 'ORD17812006903496B061E', 'PAY17812006930058A5FF0', 1, 'MOCK', 21000, 'REFUNDING', 'MOCK-ORD17812006903496B061E', '', '{\"reason\":\"...\"}', '', '', NULL, 1, '2026-06-12 01:58:31', '2026-06-12 01:58:31');
INSERT INTO `pay_refund_order` VALUES (4, 'ORF1781526648822FD054D', 'ORD1781526648573A839F8', 'PAY1781526648667E02708', 1, 'MOCK', 10000, 'REFUNDING', 'MOCK-ORD1781526648573A839F8', '', '{\"reason\":\"商品与描述不符\"}', '', '', NULL, 1, '2026-06-15 20:30:49', '2026-06-15 20:30:49');
INSERT INTO `pay_refund_order` VALUES (5, 'ORF178152672284164F2E8', 'ORD178152672263850B15C', 'PAY17815267227274444CD', 1, 'MOCK', 10000, 'REFUNDING', 'MOCK-ORD178152672263850B15C', '', '{\"reason\":\"商品与描述不符\"}', '', '', NULL, 1, '2026-06-15 20:32:03', '2026-06-15 20:32:03');
INSERT INTO `pay_refund_order` VALUES (6, 'ORF1781528098611D0788A', 'ORD178152809766513D1B9', 'PAY178152809776537175B', 1, 'MOCK', 10000, 'REFUNDING', 'MOCK-ORD178152809766513D1B9', '', '{\"reason\":\"P4 refund\"}', '', '', NULL, 1, '2026-06-15 20:54:59', '2026-06-15 20:54:59');
INSERT INTO `pay_refund_order` VALUES (7, 'ORF1781528713716FCFAFF', 'ORD1781528713367F731BD', 'PAY1781528713480A934D9', 1, 'MOCK', 10000, 'REFUNDING', 'MOCK-ORD1781528713367F731BD', '', '{\"reason\":\"商品与描述不符\"}', '', '', NULL, 1, '2026-06-15 21:05:14', '2026-06-15 21:05:14');

-- ----------------------------
-- Table structure for pms_category
-- ----------------------------
DROP TABLE IF EXISTS `pms_category`;
CREATE TABLE `pms_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '类目ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类目名称',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父类目ID',
  `level` int NOT NULL DEFAULT 1 COMMENT '层级',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED' COMMENT '类目状态',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pms_category_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品类目表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pms_category
-- ----------------------------
INSERT INTO `pms_category` VALUES (1, '电脑办公', 0, 1, 1, 'ENABLED', 0, '2026-04-29 02:39:20', '2026-04-29 02:39:20', NULL);
INSERT INTO `pms_category` VALUES (2, '家用电器', 0, 1, 2, 'ENABLED', 0, '2026-04-29 02:39:20', '2026-04-29 02:39:20', NULL);
INSERT INTO `pms_category` VALUES (3, '个护清洁', 0, 1, 3, 'ENABLED', 0, '2026-04-29 02:39:20', '2026-04-29 02:39:20', NULL);
INSERT INTO `pms_category` VALUES (4, '食品生鲜', 0, 1, 4, 'ENABLED', 0, '2026-04-29 02:39:20', '2026-04-29 02:39:20', NULL);
INSERT INTO `pms_category` VALUES (5, '运动户外', 0, 1, 5, 'ENABLED', 0, '2026-04-29 02:39:20', '2026-04-29 02:39:20', NULL);
INSERT INTO `pms_category` VALUES (6, '服饰内衣', 0, 1, 6, 'ENABLED', 0, '2026-04-29 02:39:20', '2026-04-29 02:39:20', NULL);
INSERT INTO `pms_category` VALUES (7, '母婴用品', 0, 1, 7, 'ENABLED', 0, '2026-04-29 02:39:20', '2026-04-29 02:39:20', NULL);
INSERT INTO `pms_category` VALUES (8, '图书文创', 0, 1, 8, 'ENABLED', 0, '2026-04-29 02:39:20', '2026-04-29 02:39:20', NULL);
INSERT INTO `pms_category` VALUES (9, '家居厨具', 0, 1, 9, 'ENABLED', 0, '2026-04-29 02:39:20', '2026-04-29 02:39:20', NULL);
INSERT INTO `pms_category` VALUES (10, '手机通讯', 0, 1, 10, 'ENABLED', 0, '2026-04-29 02:39:20', '2026-04-29 02:39:20', NULL);
INSERT INTO `pms_category` VALUES (11, 'P3CAT203049', 0, 1, 999, 'DISABLED', 0, '2026-06-15 20:30:49', '2026-06-15 20:30:49', NULL);
INSERT INTO `pms_category` VALUES (12, 'P3CAT203203', 0, 1, 999, 'DISABLED', 0, '2026-06-15 20:32:02', '2026-06-15 20:32:02', NULL);

-- ----------------------------
-- Table structure for pms_product_sales_daily_stat
-- ----------------------------
DROP TABLE IF EXISTS `pms_product_sales_daily_stat`;
CREATE TABLE `pms_product_sales_daily_stat`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date` date NOT NULL COMMENT '统计日期，按订单完成时间归档',
  `spu_id` bigint NOT NULL COMMENT '商品SPU ID',
  `sku_id` bigint NOT NULL COMMENT '商品SKU ID',
  `sale_channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NORMAL' COMMENT '销售渠道：NORMAL普通，SECKILL秒杀预留',
  `completed_quantity` int NOT NULL DEFAULT 0 COMMENT '完成订单销量',
  `completed_amount_cent` bigint NOT NULL DEFAULT 0 COMMENT '完成订单销售额，单位分',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_date_sku_channel`(`stat_date` ASC, `sku_id` ASC, `sale_channel` ASC) USING BTREE,
  INDEX `idx_spu_date`(`spu_id` ASC, `stat_date` ASC) USING BTREE,
  INDEX `idx_sku_date`(`sku_id` ASC, `stat_date` ASC) USING BTREE,
  INDEX `idx_channel_date`(`sale_channel` ASC, `stat_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品每日完成销量统计表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pms_product_sales_daily_stat
-- ----------------------------
INSERT INTO `pms_product_sales_daily_stat` VALUES (1, '2026-06-10', 1, 1, 'NORMAL', 5, 50000, '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `pms_product_sales_daily_stat` VALUES (2, '2026-06-10', 8, 8, 'NORMAL', 1, 18000, '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `pms_product_sales_daily_stat` VALUES (3, '2026-06-10', 10, 10, 'NORMAL', 1, 20000, '2026-06-10 10:00:00', '2026-06-10 10:00:00');
INSERT INTO `pms_product_sales_daily_stat` VALUES (4, '2026-06-10', 11, 11, 'NORMAL', 1, 21000, '2026-06-10 15:19:43', '2026-06-10 15:19:43');
INSERT INTO `pms_product_sales_daily_stat` VALUES (5, '2026-06-15', 1, 1, 'NORMAL', 3, 30000, '2026-06-15 18:38:22', '2026-06-15 21:05:13');
INSERT INTO `pms_product_sales_daily_stat` VALUES (6, '2026-06-15', 3, 3, 'NORMAL', 1, 13000, '2026-06-15 19:54:09', '2026-06-15 19:54:09');
INSERT INTO `pms_product_sales_daily_stat` VALUES (7, '2026-06-15', 12, 12, 'NORMAL', 2, 44000, '2026-06-15 19:54:09', '2026-06-15 19:54:09');
INSERT INTO `pms_product_sales_daily_stat` VALUES (8, '2026-06-16', 1, 1, 'NORMAL', 2, 20000, '2026-06-16 00:48:41', '2026-06-16 00:55:55');

-- ----------------------------
-- Table structure for pms_product_sales_stat_event
-- ----------------------------
DROP TABLE IF EXISTS `pms_product_sales_stat_event`;
CREATE TABLE `pms_product_sales_stat_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件唯一键，如 ORDER_COMPLETED:订单号',
  `event_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件类型',
  `biz_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '业务单号',
  `handled_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_event_key`(`event_key` ASC) USING BTREE,
  INDEX `idx_biz_no`(`biz_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品销售统计事件幂等表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pms_product_sales_stat_event
-- ----------------------------
INSERT INTO `pms_product_sales_stat_event` VALUES (1, 'ORDER_COMPLETED:RCT202606100010', 'ORDER_COMPLETED', 'RCT202606100010', '2026-06-10 10:00:00');
INSERT INTO `pms_product_sales_stat_event` VALUES (2, 'ORDER_COMPLETED:ORD1781075976142E01526', 'ORDER_COMPLETED', 'ORD1781075976142E01526', '2026-06-10 15:19:43');
INSERT INTO `pms_product_sales_stat_event` VALUES (3, 'ORDER_COMPLETED:ORD178151990140736FC43', 'ORDER_COMPLETED', 'ORD178151990140736FC43', '2026-06-15 18:38:22');
INSERT INTO `pms_product_sales_stat_event` VALUES (4, 'ORDER_COMPLETED:ORD17815228604692C7570', 'ORDER_COMPLETED', 'ORD17815228604692C7570', '2026-06-15 19:27:43');
INSERT INTO `pms_product_sales_stat_event` VALUES (5, 'ORDER_COMPLETED:ORD1781524417974BA4CFF', 'ORDER_COMPLETED', 'ORD1781524417974BA4CFF', '2026-06-15 19:54:09');
INSERT INTO `pms_product_sales_stat_event` VALUES (6, 'ORDER_COMPLETED:ORD1781528713367F731BD', 'ORDER_COMPLETED', 'ORD1781528713367F731BD', '2026-06-15 21:05:13');
INSERT INTO `pms_product_sales_stat_event` VALUES (7, 'ORDER_COMPLETED:ORD17815421181506A2B36', 'ORDER_COMPLETED', 'ORD17815421181506A2B36', '2026-06-16 00:48:41');
INSERT INTO `pms_product_sales_stat_event` VALUES (8, 'ORDER_COMPLETED:ORD17815425521398BF4BA', 'ORDER_COMPLETED', 'ORD17815425521398BF4BA', '2026-06-16 00:55:55');

-- ----------------------------
-- Table structure for pms_sku
-- ----------------------------
DROP TABLE IF EXISTS `pms_sku`;
CREATE TABLE `pms_sku`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
  `spu_id` bigint NOT NULL COMMENT '所属SPU ID',
  `sku_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SKU编码',
  `sku_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SKU名称',
  `spec_json` json NULL COMMENT '规格JSON',
  `sale_price_cent` bigint NOT NULL COMMENT '销售价分',
  `origin_price_cent` bigint NOT NULL COMMENT '原价分',
  `sales_count` int NOT NULL DEFAULT 0 COMMENT '销量',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ONLINE' COMMENT 'SKU状态',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_pms_sku_code`(`sku_code` ASC) USING BTREE,
  INDEX `idx_pms_sku_spu_id`(`spu_id` ASC) USING BTREE,
  INDEX `idx_pms_sku_spu_deleted_id`(`spu_id` ASC, `deleted_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_pms_sku_deleted_sku_code`(`deleted_at` ASC, `sku_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品SKU表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pms_sku
-- ----------------------------
INSERT INTO `pms_sku` VALUES (1, 1, 'SKU-DEMO-0001', '星麦保温杯 默认规格', '{\"规格\": \"默认规格\", \"颜色\": \"标准色\"}', 10000, 13000, 10, 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-16 00:55:55', NULL);
INSERT INTO `pms_sku` VALUES (2, 2, 'SKU-DEMO-0002', '鹿岛空气炸锅 默认规格', '{\"规格\": \"默认规格\", \"颜色\": \"标准色\"}', 12000, 15000, 0, 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `pms_sku` VALUES (3, 3, 'SKU-DEMO-0003', '青橙护眼台灯 默认规格', '{\"规格\": \"默认规格\", \"颜色\": \"标准色\"}', 13000, 16000, 1, 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-15 19:54:09', NULL);
INSERT INTO `pms_sku` VALUES (4, 4, 'SKU-DEMO-0004', '北庭跑步鞋 默认规格', '{\"规格\": \"默认规格\", \"颜色\": \"标准色\"}', 14000, 17000, 0, 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `pms_sku` VALUES (5, 5, 'SKU-DEMO-0005', '云岚双肩包 默认规格', '{\"规格\": \"默认规格\", \"颜色\": \"标准色\"}', 15000, 18000, 0, 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `pms_sku` VALUES (6, 6, 'SKU-DEMO-0006', '山也电动牙刷 默认规格', '{\"规格\": \"默认规格\", \"颜色\": \"标准色\"}', 16000, 19000, 0, 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `pms_sku` VALUES (7, 7, 'SKU-DEMO-0007', '沐白乳胶枕 默认规格', '{\"规格\": \"默认规格\", \"颜色\": \"标准色\"}', 17000, 20000, 0, 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `pms_sku` VALUES (8, 8, 'SKU-DEMO-0008', '岚森牛奶礼盒 默认规格', '{\"规格\": \"默认规格\", \"颜色\": \"标准色\"}', 18000, 21000, 1, 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `pms_sku` VALUES (9, 9, 'SKU-DEMO-0009', '初合机械键盘 默认规格', '{\"规格\": \"默认规格\", \"颜色\": \"标准色\"}', 19000, 22000, 0, 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `pms_sku` VALUES (10, 10, 'SKU-DEMO-0010', '鲸选防晒霜 默认规格', '{\"规格\": \"默认规格\", \"颜色\": \"标准色\"}', 20000, 23000, 1, 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `pms_sku` VALUES (11, 11, 'SKU-DEMO-0011', '有栖行李箱 默认规格', '{\"规格\": \"默认规格\", \"颜色\": \"标准色\"}', 21000, 24000, 1, 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 15:19:43', NULL);
INSERT INTO `pms_sku` VALUES (12, 12, 'SKU-DEMO-0012', '松禾无线耳机 默认规格', '{\"规格\": \"默认规格\", \"颜色\": \"标准色\"}', 22000, 25000, 2, 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-15 19:54:09', NULL);
INSERT INTO `pms_sku` VALUES (13, 13, 'SKU-拉布布-玩偶-1', '拉布布-玩偶-默认规格', '{\"规格\": \"默认规格\"}', 1000, 10000, 200, 'ONLINE', 0, '2026-06-19 14:16:53', '2026-06-19 14:20:12', NULL);
INSERT INTO `pms_sku` VALUES (14, 13, 'SKU-拉布布-玩偶-2', '拉布布-玩偶-大号', '{\"规格\": \"大号\"}', 2000, 20000, 200, 'ONLINE', 0, '2026-06-19 14:18:50', '2026-06-19 14:20:14', NULL);

-- ----------------------------
-- Table structure for pms_spu
-- ----------------------------
DROP TABLE IF EXISTS `pms_spu`;
CREATE TABLE `pms_spu`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'SPU ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品名称',
  `category_id` bigint NOT NULL COMMENT '所属类目ID',
  `main_image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '主图地址',
  `album_images_json` json NULL COMMENT '图集JSON',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '商品描述',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ONLINE' COMMENT '商品状态',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_pms_spu_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_pms_spu_status_category_deleted_id`(`status` ASC, `category_id` ASC, `deleted_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_pms_spu_category_deleted_id`(`category_id` ASC, `deleted_at` ASC, `id` ASC) USING BTREE,
  INDEX `idx_pms_spu_deleted_id`(`deleted_at` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品SPU表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of pms_spu
-- ----------------------------
INSERT INTO `pms_spu` VALUES (1, '星麦保温杯', 9, '/uploads/product/2026/06/10/3b8baa11c6d949dd9b4cf84611438534.png', '[\"/uploads/product/demo/spu-01-1.png\", \"/uploads/product/demo/spu-01-2.png\"]', '演示商品：星麦保温杯，用于商城浏览、下单、支付、售后、库存、对账测试。', 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 03:25:25', NULL);
INSERT INTO `pms_spu` VALUES (2, '鹿岛空气炸锅', 2, '/uploads/product/2026/06/10/a83bd60d6f1f4bba818788d9f99210f6.png', '[\"/uploads/product/demo/spu-02-1.png\", \"/uploads/product/demo/spu-02-2.png\"]', '演示商品：鹿岛空气炸锅，用于商城浏览、下单、支付、售后、库存、对账测试。', 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 03:25:19', NULL);
INSERT INTO `pms_spu` VALUES (3, '青橙护眼台灯', 1, '/uploads/product/2026/06/10/48e6e507f29f475790e6c1a459270605.png', '[\"/uploads/product/demo/spu-03-1.png\", \"/uploads/product/demo/spu-03-2.png\"]', '演示商品：青橙护眼台灯，用于商城浏览、下单、支付、售后、库存、对账测试。', 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 03:25:12', NULL);
INSERT INTO `pms_spu` VALUES (4, '北庭跑步鞋', 5, '/uploads/product/2026/06/10/59908e0eff0c4e7888e7145c44c4cf28.jpeg', '[\"/uploads/product/demo/spu-04-1.png\", \"/uploads/product/demo/spu-04-2.png\"]', '演示商品：北庭跑步鞋，用于商城浏览、下单、支付、售后、库存、对账测试。', 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 03:25:05', NULL);
INSERT INTO `pms_spu` VALUES (5, '云岚双肩包', 5, '/uploads/product/2026/06/10/3ea59cfc5e744be58ef88bde491c5d78.jpeg', '[\"/uploads/product/demo/spu-05-1.png\", \"/uploads/product/demo/spu-05-2.png\"]', '演示商品：云岚双肩包，用于商城浏览、下单、支付、售后、库存、对账测试。', 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 03:24:52', NULL);
INSERT INTO `pms_spu` VALUES (6, '山也电动牙刷', 3, '/uploads/product/2026/06/10/2741559b83754426bc90663c1c65c04f.jpeg', '[\"/uploads/product/demo/spu-06-1.png\", \"/uploads/product/demo/spu-06-2.png\"]', '演示商品：山也电动牙刷，用于商城浏览、下单、支付、售后、库存、对账测试。', 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 03:24:43', NULL);
INSERT INTO `pms_spu` VALUES (7, '沐白乳胶枕', 9, '/uploads/product/2026/06/10/33b22da0a55b4e5ba0bf285ada3f866e.jpeg', '[\"/uploads/product/demo/spu-07-1.png\", \"/uploads/product/demo/spu-07-2.png\"]', '演示商品：沐白乳胶枕，用于商城浏览、下单、支付、售后、库存、对账测试。', 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 03:24:37', NULL);
INSERT INTO `pms_spu` VALUES (8, '岚森牛奶礼盒', 4, '/uploads/product/2026/06/10/ece29ebb73a043138b45b5660aa721a3.jpeg', '[\"/uploads/product/demo/spu-08-1.png\", \"/uploads/product/demo/spu-08-2.png\"]', '演示商品：岚森牛奶礼盒，用于商城浏览、下单、支付、售后、库存、对账测试。', 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 03:24:31', NULL);
INSERT INTO `pms_spu` VALUES (9, '初合机械键盘', 1, '/uploads/product/2026/06/10/3443574991734406bb2f7bfe60b86a8e.jpeg', '[\"/uploads/product/demo/spu-09-1.png\", \"/uploads/product/demo/spu-09-2.png\"]', '演示商品：初合机械键盘，用于商城浏览、下单、支付、售后、库存、对账测试。', 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 03:24:22', NULL);
INSERT INTO `pms_spu` VALUES (10, '鲸选防晒霜', 3, '/uploads/product/2026/06/10/d72a34d879a14db3b42abe9e310519bd.jpeg', '[\"/uploads/product/demo/spu-10-1.png\", \"/uploads/product/demo/spu-10-2.png\"]', '演示商品：鲸选防晒霜，用于商城浏览、下单、支付、售后、库存、对账测试。', 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 03:24:16', NULL);
INSERT INTO `pms_spu` VALUES (11, '有栖行李箱', 5, '/uploads/product/2026/06/10/a5ac2983f753438f9ad6cd35239fa671.png', '[\"/uploads/product/demo/spu-11-1.png\", \"/uploads/product/demo/spu-11-2.png\"]', '演示商品：有栖行李箱，用于商城浏览、下单、支付、售后、库存、对账测试。', 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 03:24:08', NULL);
INSERT INTO `pms_spu` VALUES (12, '松禾无线耳机', 10, '/uploads/product/2026/06/10/562c08ac753e4bb0a7bbc7d6eabe7d9c.jpg', '[\"/uploads/product/demo/spu-12-1.png\", \"/uploads/product/demo/spu-12-2.png\"]', '演示商品：松禾无线耳机，用于商城浏览、下单、支付、售后、库存、对账测试。', 'ONLINE', 0, '2026-01-01 09:00:00', '2026-06-10 02:50:46', NULL);
INSERT INTO `pms_spu` VALUES (13, '拉布布-玩偶', 1, 'http://127.0.0.1:9090/uploads/product/2026/06/16/d2594ea8625e44a986eba8f9476e91e5.png', '[]', '<p>拉布布玩偶，你的友好选择</p>', 'ONLINE', 0, '2026-06-15 20:54:57', '2026-06-19 14:18:54', NULL);

-- ----------------------------
-- Table structure for ums_admin
-- ----------------------------
DROP TABLE IF EXISTS `ums_admin`;
CREATE TABLE `ums_admin`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '关联用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '管理员账号',
  `password_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码哈希',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '管理员昵称',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUPER_ADMIN' COMMENT '角色编码',
  `permissions_json` json NULL COMMENT '权限集合JSON',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED' COMMENT '账户状态',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ums_admin_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ums_admin
-- ----------------------------
INSERT INTO `ums_admin` VALUES (1, NULL, 'admin', '123456', '超级管理员', 'SUPER_ADMIN', '[\"dashboard:view\", \"dashboard:operations:view\", \"dashboard:finance:view\", \"dashboard:warehouse:view\", \"dashboard:products:view\", \"admin:view\", \"admin:create\", \"admin:update\", \"admin:disable\", \"role:view\", \"role:manage\", \"permission:view\", \"permission:assign\", \"log:operation:view\", \"user:view\", \"user:detail:view\", \"user:address:view\", \"user:edit\", \"user:disable\", \"user:export\", \"product:view\", \"product:detail:view\", \"product:create\", \"product:update\", \"product:on_sale\", \"product:off_sale\", \"product:status:update\", \"product:sales:view\", \"product:sales-threshold:view\", \"product:sales-threshold:config\", \"product:violation:handle\", \"category:manage\", \"stock:view\", \"stock:log:view\", \"stock:adjust\", \"stock:policy:update\", \"stock:warning:view\", \"stock:warning:handle\", \"stock:reconcile:view\", \"stock:reconcile:check\", \"stock:reconcile:repair\", \"order:view\", \"order:detail:view\", \"order:remark\", \"order:receiver:update\", \"order:ship\", \"order:close\", \"order:exception:view\", \"order:exception:handle\", \"order:payment-exception:handle\", \"order:confirm-paid\", \"order:sku:switch\", \"order:log:view\", \"aftersale:view\", \"aftersale:detail:view\", \"aftersale:audit\", \"aftersale:review\", \"aftersale:refund:view\", \"refund:view\", \"refund:execute\", \"refund:sync\", \"finance:view\", \"payment:view\", \"payment:detail:view\", \"payment:close\", \"payment:sync\", \"payment:repair\", \"payment:callback:view\", \"reconciliation:view\", \"reconciliation:task:create\", \"reconciliation:task:run\", \"reconciliation:task:archive\", \"reconciliation:bill:import\", \"reconciliation:diff:handle\", \"reconciliation:diff:repair\", \"reconciliation:hanging:follow\", \"reconciliation:handle\"]', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-17 10:00:00', NULL);
INSERT INTO `ums_admin` VALUES (2, NULL, 'finance', '123456', '财务人员', 'FINANCE_OPERATOR', '[\"dashboard:view\", \"dashboard:finance:view\", \"finance:view\", \"payment:view\", \"payment:detail:view\", \"payment:sync\", \"payment:repair\", \"payment:close\", \"payment:callback:view\", \"refund:view\", \"refund:sync\", \"refund:execute\", \"reconciliation:view\", \"reconciliation:task:create\", \"reconciliation:task:run\", \"reconciliation:task:archive\", \"reconciliation:bill:import\", \"reconciliation:diff:handle\", \"reconciliation:diff:repair\", \"reconciliation:hanging:follow\", \"reconciliation:handle\", \"order:view\", \"order:detail:view\", \"order:exception:view\", \"order:log:view\", \"aftersale:view\", \"aftersale:detail:view\", \"aftersale:refund:view\"]', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-17 10:00:00', NULL);
INSERT INTO `ums_admin` VALUES (3, NULL, 'order', '123456', '订单运营', 'ORDER_OPERATOR', '[\"dashboard:view\", \"dashboard:operations:view\", \"user:view\", \"user:detail:view\", \"user:address:view\", \"order:view\", \"order:detail:view\", \"order:remark\", \"order:receiver:update\", \"order:ship\", \"order:close\", \"order:exception:view\", \"order:exception:handle\", \"order:payment-exception:handle\", \"order:confirm-paid\", \"order:sku:switch\", \"order:log:view\", \"aftersale:view\", \"aftersale:detail:view\", \"aftersale:audit\", \"aftersale:review\", \"aftersale:refund:view\", \"product:view\", \"product:detail:view\", \"payment:view\", \"payment:detail:view\", \"refund:view\"]', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-17 10:00:00', NULL);
INSERT INTO `ums_admin` VALUES (4, NULL, 'stock', '123456', '仓储人员', 'WAREHOUSE_OPERATOR', '[\"dashboard:view\", \"dashboard:warehouse:view\", \"stock:view\", \"stock:log:view\", \"stock:adjust\", \"stock:policy:update\", \"stock:warning:view\", \"stock:warning:handle\", \"stock:reconcile:view\", \"stock:reconcile:check\", \"stock:reconcile:repair\", \"order:view\", \"order:detail:view\", \"order:ship\", \"order:log:view\", \"product:view\", \"product:detail:view\"]', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-17 10:00:00', NULL);
INSERT INTO `ums_admin` VALUES (5, NULL, 'product', '123456', '商品运营', 'PRODUCT_OPERATOR', '[\"dashboard:view\", \"dashboard:products:view\", \"product:view\", \"product:detail:view\", \"product:create\", \"product:update\", \"product:on_sale\", \"product:off_sale\", \"product:status:update\", \"product:sales:view\", \"product:sales-threshold:view\", \"product:sales-threshold:config\", \"product:violation:handle\", \"category:manage\", \"stock:view\", \"stock:warning:view\", \"order:view\", \"order:detail:view\"]', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-17 10:00:00', NULL);

-- ----------------------------
-- Table structure for ums_admin_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `ums_admin_operation_log`;
CREATE TABLE `ums_admin_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `operator_admin_id` bigint NOT NULL COMMENT '操作管理员ID',
  `operator_username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作管理员账号',
  `operation_module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作模块',
  `operation_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `operation_content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作内容',
  `operation_result` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '操作结果',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ums_admin_operation_log_admin_id`(`operator_admin_id` ASC) USING BTREE,
  INDEX `idx_ums_admin_operation_log_module`(`operation_module` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 141 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '运营后台操作日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ums_admin_operation_log
-- ----------------------------
INSERT INTO `ums_admin_operation_log` VALUES (1, 1, 'admin', 'SYSTEM', 'DATA_RESET', '清洗演示数据：管理员/用户/商品/SPU/SKU/库存/订单支付售后对账数据配套重建', 'SUCCESS', '2026-06-10 10:00:00');
INSERT INTO `ums_admin_operation_log` VALUES (2, 2, 'finance', 'PAY', 'RECONCILIATION_PREPARE', '生成支付/退款/对账闭环测试数据', 'SUCCESS', '2026-06-10 10:00:00');
INSERT INTO `ums_admin_operation_log` VALUES (3, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：RCT202606100009，结论=PAID_VERIFIED，建议=CONFIRM_PAID', 'SUCCESS', '2026-06-10 15:13:40');
INSERT INTO `ums_admin_operation_log` VALUES (4, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_PENDING_ACTION', '支付异常标记待处理：RCT202606100009，动作=ORDER_REPAIR_PENDING，对账记录ID=11', 'SUCCESS', '2026-06-10 15:13:41');
INSERT INTO `ums_admin_operation_log` VALUES (5, 1, 'admin', 'PAY', 'PAY_SYNC', '同步订单支付状态 orderNo=RCT202606100004', 'SUCCESS', '2026-06-10 15:13:49');
INSERT INTO `ums_admin_operation_log` VALUES (6, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_MARK', '标记订单支付异常：RCT202606100009', 'SUCCESS', '2026-06-10 15:14:28');
INSERT INTO `ums_admin_operation_log` VALUES (7, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：RCT202606100009，结论=PAID_VERIFIED，建议=CONFIRM_PAID', 'SUCCESS', '2026-06-10 15:14:32');
INSERT INTO `ums_admin_operation_log` VALUES (8, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_PENDING_ACTION', '支付异常标记待处理：RCT202606100009，动作=ORDER_REPAIR_PENDING，对账记录ID=12', 'SUCCESS', '2026-06-10 15:14:51');
INSERT INTO `ums_admin_operation_log` VALUES (9, 1, 'admin', 'PAY', 'REFUND_SYNC', '同步退款状态 orderNo=RCT202606100008，refundNo=RFDRCT202606100008', 'SUCCESS', '2026-06-10 15:18:17');
INSERT INTO `ums_admin_operation_log` VALUES (10, 1, 'admin', 'AFTERSALE', 'AFTERSALE_REJECT', '审核驳回售后单：AFT1781076032228AB7729，原因：商家审核驳回', 'SUCCESS', '2026-06-10 15:20:43');
INSERT INTO `ums_admin_operation_log` VALUES (11, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_MARK', '标记订单支付异常：ORD17810762227909DD3A7', 'SUCCESS', '2026-06-10 15:24:06');
INSERT INTO `ums_admin_operation_log` VALUES (12, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：ORD17810762227909DD3A7，结论=NO_PAY_ORDER，建议=RESTORE_PENDING_PAYMENT', 'SUCCESS', '2026-06-10 15:24:20');
INSERT INTO `ums_admin_operation_log` VALUES (13, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：ORD17810762227909DD3A7，结论=NO_PAY_ORDER，建议=RESTORE_PENDING_PAYMENT', 'SUCCESS', '2026-06-10 15:24:51');
INSERT INTO `ums_admin_operation_log` VALUES (14, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_RESTORE_PENDING', '恢复支付异常订单为待支付：ORD17810762227909DD3A7', 'SUCCESS', '2026-06-10 15:24:52');
INSERT INTO `ums_admin_operation_log` VALUES (15, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_MARK', '标记订单支付异常：ORD17810762227909DD3A7', 'SUCCESS', '2026-06-10 15:26:05');
INSERT INTO `ums_admin_operation_log` VALUES (16, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：ORD17810762227909DD3A7，结论=UNPAID_VERIFIED，建议=RESTORE_PENDING_PAYMENT', 'SUCCESS', '2026-06-10 15:26:10');
INSERT INTO `ums_admin_operation_log` VALUES (17, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：ORD17810762227909DD3A7，结论=UNPAID_VERIFIED，建议=RESTORE_PENDING_PAYMENT', 'SUCCESS', '2026-06-10 15:26:26');
INSERT INTO `ums_admin_operation_log` VALUES (18, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：ORD17810762227909DD3A7，结论=UNPAID_VERIFIED，建议=RESTORE_PENDING_PAYMENT', 'SUCCESS', '2026-06-10 15:27:12');
INSERT INTO `ums_admin_operation_log` VALUES (19, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_RESTORE_PENDING', '恢复支付异常订单为待支付：ORD17810762227909DD3A7', 'SUCCESS', '2026-06-10 15:27:14');
INSERT INTO `ums_admin_operation_log` VALUES (20, 1, 'admin', 'ORDER', 'ORDER_SHIP', '发货订单：ORD1781076001361016574', 'SUCCESS', '2026-06-10 15:51:42');
INSERT INTO `ums_admin_operation_log` VALUES (21, 1, 'admin', 'AFTERSALE', 'AFTERSALE_APPROVE', '审核通过并生成退款单：AFT17810780633198F8972，退款单号：ORF1781078073650ED3D88', 'SUCCESS', '2026-06-10 15:54:33');
INSERT INTO `ums_admin_operation_log` VALUES (22, 1, 'admin', 'PAY', 'REFUND_SYNC', '同步退款状态 orderNo=RCT202606100009，refundNo=ORF1781078073650ED3D88', 'SUCCESS', '2026-06-10 15:54:47');
INSERT INTO `ums_admin_operation_log` VALUES (23, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_MARK', '标记订单支付异常：ORD1781076001361016574', 'SUCCESS', '2026-06-10 16:26:49');
INSERT INTO `ums_admin_operation_log` VALUES (24, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：ORD1781076001361016574，结论=PAID_VERIFIED，建议=CONFIRM_PAID', 'SUCCESS', '2026-06-10 16:27:00');
INSERT INTO `ums_admin_operation_log` VALUES (25, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_PENDING_ACTION', '支付异常标记待处理：ORD1781076001361016574，动作=ORDER_REPAIR_PENDING，对账记录ID=15', 'SUCCESS', '2026-06-10 16:27:07');
INSERT INTO `ums_admin_operation_log` VALUES (26, 1, 'admin', 'PAY', 'PAY_REPAIR', '补偿订单支付状态 orderNo=ORD1781076001361016574', 'SUCCESS', '2026-06-10 16:27:18');
INSERT INTO `ums_admin_operation_log` VALUES (27, 1, 'admin', 'ORDER', 'ORDER_SHIP', '发货订单：ORD1781076001361016574', 'SUCCESS', '2026-06-10 16:30:41');
INSERT INTO `ums_admin_operation_log` VALUES (28, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_MARK', '标记订单支付异常：ORD1781076001361016574', 'SUCCESS', '2026-06-10 16:31:42');
INSERT INTO `ums_admin_operation_log` VALUES (29, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：ORD1781076001361016574，结论=PAID_VERIFIED，建议=CONFIRM_PAID', 'SUCCESS', '2026-06-10 16:31:45');
INSERT INTO `ums_admin_operation_log` VALUES (30, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_PENDING_ACTION', '支付异常标记待处理：ORD1781076001361016574，动作=ORDER_REPAIR_PENDING，对账记录ID=16', 'SUCCESS', '2026-06-10 16:31:54');
INSERT INTO `ums_admin_operation_log` VALUES (31, 1, 'admin', 'PAY', 'PAY_REPAIR', '补偿订单支付状态 orderNo=ORD1781076001361016574', 'SUCCESS', '2026-06-10 16:32:03');
INSERT INTO `ums_admin_operation_log` VALUES (32, 1, 'admin', 'PAY', 'PAY_SYNC', '同步订单支付状态 orderNo=RCT202606100005', 'SUCCESS', '2026-06-10 16:32:48');
INSERT INTO `ums_admin_operation_log` VALUES (33, 1, 'admin', 'PAY', 'PAY_SYNC', '同步订单支付状态 orderNo=RCT202606100004', 'SUCCESS', '2026-06-10 16:32:52');
INSERT INTO `ums_admin_operation_log` VALUES (34, 1, 'admin', 'PAY', 'PAY_SYNC', '同步订单支付状态 orderNo=RCT202606100004', 'SUCCESS', '2026-06-10 16:34:47');
INSERT INTO `ums_admin_operation_log` VALUES (35, 1, 'admin', 'PAY', 'PAY_SYNC', '同步订单支付状态 orderNo=RCT202606100004', 'SUCCESS', '2026-06-10 16:39:47');
INSERT INTO `ums_admin_operation_log` VALUES (36, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_MARK', '标记订单支付异常：ORD17810814566188EA786', 'SUCCESS', '2026-06-10 16:51:19');
INSERT INTO `ums_admin_operation_log` VALUES (37, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：ORD17810814566188EA786，结论=UNPAID_VERIFIED，建议=RESTORE_PENDING_PAYMENT', 'SUCCESS', '2026-06-10 16:51:21');
INSERT INTO `ums_admin_operation_log` VALUES (38, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_RESTORE_PENDING', '恢复支付异常订单为待支付：ORD17810814566188EA786', 'SUCCESS', '2026-06-10 16:51:34');
INSERT INTO `ums_admin_operation_log` VALUES (39, 1, 'admin', 'ORDER', 'ORDER_CANCEL', '取消订单：ORD17810814566188EA786', 'SUCCESS', '2026-06-10 16:51:48');
INSERT INTO `ums_admin_operation_log` VALUES (40, 1, 'admin', 'STOCK', 'STOCK_RECONCILIATION_REPAIR', '修复库存对账记录 id=2，sku=2，备注=对账人员批量确认修复', 'SUCCESS', '2026-06-10 16:52:18');
INSERT INTO `ums_admin_operation_log` VALUES (41, 1, 'admin', 'STOCK', 'STOCK_RECONCILIATION_REPAIR', '修复库存对账记录 id=3，sku=1，备注=对账人员批量确认修复', 'SUCCESS', '2026-06-10 16:52:19');
INSERT INTO `ums_admin_operation_log` VALUES (42, 1, 'admin', 'STOCK', 'STOCK_RECONCILIATION_REPAIR', '修复库存对账记录 id=4，sku=3，备注=对账人员批量确认修复', 'SUCCESS', '2026-06-10 16:52:19');
INSERT INTO `ums_admin_operation_log` VALUES (43, 1, 'admin', 'PAY', 'PAY_RECONCILE_RECORD_HANDLE', '处置对账记录 id=14，orderNo=RCT202606100009，动作=SYNC_PAY_STATUS，原因=STATUS_MISMATCH', 'SUCCESS', '2026-06-10 16:53:06');
INSERT INTO `ums_admin_operation_log` VALUES (44, 1, 'admin', 'PAY', 'PAY_RECONCILE_RECORD_HANDLE', '处置对账记录 id=13，orderNo=RCT202606100008，动作=SYNC_PAY_STATUS，原因=.', 'SUCCESS', '2026-06-10 16:53:33');
INSERT INTO `ums_admin_operation_log` VALUES (45, 1, 'admin', 'STOCK', 'STOCK_CONSISTENCY_CHECK', '发起库存一致性校验 sku=1，结果=CONSISTENT', 'SUCCESS', '2026-06-10 17:43:42');
INSERT INTO `ums_admin_operation_log` VALUES (46, 1, 'admin', 'STOCK', 'STOCK_CONSISTENCY_CHECK', '发起库存一致性校验 sku=1，结果=INCONSISTENT', 'SUCCESS', '2026-06-10 18:02:26');
INSERT INTO `ums_admin_operation_log` VALUES (47, 1, 'admin', 'STOCK', 'STOCK_RECONCILIATION_REPAIR', '修复库存对账记录 id=6，sku=1，备注=线上人工对账页触发库存修复', 'SUCCESS', '2026-06-10 18:02:38');
INSERT INTO `ums_admin_operation_log` VALUES (48, 1, 'admin', 'STOCK', 'STOCK_CONSISTENCY_CHECK', '发起库存一致性校验 sku=1，结果=INCONSISTENT', 'SUCCESS', '2026-06-10 18:14:36');
INSERT INTO `ums_admin_operation_log` VALUES (49, 1, 'admin', 'STOCK', 'STOCK_CONSISTENCY_CHECK', '发起库存一致性校验 sku=2，结果=INCONSISTENT', 'SUCCESS', '2026-06-10 18:14:38');
INSERT INTO `ums_admin_operation_log` VALUES (50, 1, 'admin', 'STOCK', 'STOCK_CONSISTENCY_CHECK', '发起库存一致性校验 sku=3，结果=CONSISTENT', 'SUCCESS', '2026-06-10 18:14:41');
INSERT INTO `ums_admin_operation_log` VALUES (51, 1, 'admin', 'STOCK', 'STOCK_RECONCILIATION_REPAIR', '修复库存对账记录 id=8，sku=2，备注=库存对账页触发批量修复', 'SUCCESS', '2026-06-10 18:15:33');
INSERT INTO `ums_admin_operation_log` VALUES (52, 1, 'admin', 'STOCK', 'STOCK_RECONCILIATION_REPAIR', '修复库存对账记录 id=7，sku=1，备注=库存对账页触发批量修复', 'SUCCESS', '2026-06-10 18:15:33');
INSERT INTO `ums_admin_operation_log` VALUES (53, 1, 'admin', 'STOCK', 'STOCK_CONSISTENCY_CHECK', '发起库存一致性校验 sku=1，结果=CONSISTENT', 'SUCCESS', '2026-06-10 18:37:48');
INSERT INTO `ums_admin_operation_log` VALUES (54, 1, 'admin', 'STOCK', 'STOCK_CONSISTENCY_CHECK', '发起库存一致性校验 sku=1，结果=CONSISTENT', 'SUCCESS', '2026-06-10 18:38:06');
INSERT INTO `ums_admin_operation_log` VALUES (55, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_TASK_CREATE', '创建线上对账任务：RC20260610MOCK82981', 'SUCCESS', '2026-06-10 18:39:42');
INSERT INTO `ums_admin_operation_log` VALUES (56, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_LOCAL_BILL_GENERATE', '生成本地账单快照：taskId=1', 'SUCCESS', '2026-06-10 18:40:00');
INSERT INTO `ums_admin_operation_log` VALUES (57, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_LOCAL_BILL_GENERATE', '生成本地账单快照：taskId=1', 'SUCCESS', '2026-06-10 18:51:56');
INSERT INTO `ums_admin_operation_log` VALUES (58, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_MOCK_CHANNEL_BILL_GENERATE', '生成Mock渠道账单：taskId=1，mode=FULL_TEST', 'SUCCESS', '2026-06-10 18:56:13');
INSERT INTO `ums_admin_operation_log` VALUES (59, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_MATCH', '执行自动勾兑：taskId=1，差异=6', 'SUCCESS', '2026-06-10 18:56:19');
INSERT INTO `ums_admin_operation_log` VALUES (60, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_TASK_CREATE', '创建线上对账任务：RC20260610MOCK62798', 'SUCCESS', '2026-06-10 18:57:42');
INSERT INTO `ums_admin_operation_log` VALUES (61, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_TASK_CREATE', '创建线上对账任务：RC20260609MOCK38351', 'SUCCESS', '2026-06-10 19:15:38');
INSERT INTO `ums_admin_operation_log` VALUES (62, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_LOCAL_BILL_GENERATE', '生成本地账单快照：taskId=3', 'SUCCESS', '2026-06-10 19:18:14');
INSERT INTO `ums_admin_operation_log` VALUES (63, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_MOCK_CHANNEL_BILL_GENERATE', '生成Mock渠道账单：taskId=3，mode=FULL_TEST', 'SUCCESS', '2026-06-10 19:18:22');
INSERT INTO `ums_admin_operation_log` VALUES (64, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_LOCAL_BILL_GENERATE', '生成本地账单快照：taskId=3', 'SUCCESS', '2026-06-10 19:20:54');
INSERT INTO `ums_admin_operation_log` VALUES (65, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_TASK_CREATE', '创建线上对账任务：RC20260609MOCK35392', 'SUCCESS', '2026-06-10 19:25:35');
INSERT INTO `ums_admin_operation_log` VALUES (66, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_TASK_CREATE', '创建线上对账任务：RC20260610MOCK59944', 'SUCCESS', '2026-06-11 15:49:20');
INSERT INTO `ums_admin_operation_log` VALUES (67, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_LOCAL_BILL_GENERATE', '生成本地账单快照：taskId=5', 'SUCCESS', '2026-06-11 15:49:23');
INSERT INTO `ums_admin_operation_log` VALUES (68, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_MOCK_CHANNEL_BILL_GENERATE', '生成Mock渠道账单：taskId=5，mode=FULL_TEST', 'SUCCESS', '2026-06-11 15:49:36');
INSERT INTO `ums_admin_operation_log` VALUES (69, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_MATCH', '执行自动勾兑：taskId=5，差异=6', 'SUCCESS', '2026-06-11 15:49:47');
INSERT INTO `ums_admin_operation_log` VALUES (70, 1, 'admin', 'STOCK', 'STOCK_RECONCILIATION_REPAIR', '修复库存对账记录 id=10，sku=12，备注=库存对账页触发单条修复', 'SUCCESS', '2026-06-11 15:51:39');
INSERT INTO `ums_admin_operation_log` VALUES (71, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_TASK_COMPLETE', '归档线上对账任务：RC20260609MOCK35392', 'SUCCESS', '2026-06-11 22:20:50');
INSERT INTO `ums_admin_operation_log` VALUES (72, 1, 'admin', 'AFTERSALE', 'AFTERSALE_APPROVE', '审核通过并生成退款单：AFT1781200699204F54809，退款单号：ORF17812007111280C0DE2', 'SUCCESS', '2026-06-12 01:58:31');
INSERT INTO `ums_admin_operation_log` VALUES (73, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_MATCH', '执行自动勾兑：taskId=5，差异=6', 'SUCCESS', '2026-06-13 19:57:05');
INSERT INTO `ums_admin_operation_log` VALUES (74, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_DIFF_HANDLE', '处理线上对账差异：diffId=48，动作=MARK_TEST_FLOW_VOID', 'SUCCESS', '2026-06-13 19:57:19');
INSERT INTO `ums_admin_operation_log` VALUES (75, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_DIFF_HANDLE', '处理线上对账差异：diffId=33，动作=HANG', 'SUCCESS', '2026-06-14 21:02:34');
INSERT INTO `ums_admin_operation_log` VALUES (76, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_DIFF_HANDLE', '处理线上对账差异：diffId=34，动作=HANG', 'SUCCESS', '2026-06-14 21:10:24');
INSERT INTO `ums_admin_operation_log` VALUES (77, 1, 'admin', 'PAY', 'PAY_SYNC', '同步订单支付状态 orderNo=RCT202606100008', 'SUCCESS', '2026-06-14 21:13:52');
INSERT INTO `ums_admin_operation_log` VALUES (78, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_DIFF_HANDLE', '处理线上对账差异：diffId=41，动作=SYNC_PAY_STATUS', 'SUCCESS', '2026-06-14 21:13:53');
INSERT INTO `ums_admin_operation_log` VALUES (79, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_DIFF_HANDLE', '处理线上对账差异：diffId=34，动作=HANG', 'SUCCESS', '2026-06-14 21:34:06');
INSERT INTO `ums_admin_operation_log` VALUES (80, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_DIFF_HANDLE', '处理线上对账差异：diffId=35，动作=HANG', 'SUCCESS', '2026-06-14 21:56:10');
INSERT INTO `ums_admin_operation_log` VALUES (81, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_DIFF_HANDLE', '处理线上对账差异：diffId=40，动作=MARK_DONE', 'SUCCESS', '2026-06-14 21:57:34');
INSERT INTO `ums_admin_operation_log` VALUES (82, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_TASK_COMPLETE', '归档线上对账任务：RC20260610MOCK59944', 'SUCCESS', '2026-06-14 21:58:06');
INSERT INTO `ums_admin_operation_log` VALUES (83, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_TASK_CREATE', '创建线上对账任务：RC20260614ALIPAY37880', 'SUCCESS', '2026-06-14 23:12:17');
INSERT INTO `ums_admin_operation_log` VALUES (84, 1, 'admin', 'SYSTEM', 'ACCOUNT_CREATE', '创建运营账号：审计人员，角色=AUDIT_OPERATOR', 'SUCCESS', '2026-06-15 18:26:08');
INSERT INTO `ums_admin_operation_log` VALUES (85, 1, 'admin', 'ORDER', 'ORDER_SHIP', '发货订单：ORD178151990140736FC43', 'SUCCESS', '2026-06-15 18:38:22');
INSERT INTO `ums_admin_operation_log` VALUES (86, 1, 'admin', 'ORDER', 'ORDER_COMPLETE', '完结订单：ORD178151990140736FC43', 'SUCCESS', '2026-06-15 18:38:22');
INSERT INTO `ums_admin_operation_log` VALUES (87, 1, 'admin', 'STOCK', 'STOCK_CONSISTENCY_CHECK', '发起库存一致性校验 sku=1，结果=CONSISTENT', 'SUCCESS', '2026-06-15 19:27:42');
INSERT INTO `ums_admin_operation_log` VALUES (88, 1, 'admin', 'ORDER', 'ORDER_SHIP', '发货订单：ORD17815228604692C7570', 'SUCCESS', '2026-06-15 19:27:42');
INSERT INTO `ums_admin_operation_log` VALUES (89, 1, 'admin', 'ORDER', 'ORDER_COMPLETE', '完结订单：ORD17815228604692C7570', 'SUCCESS', '2026-06-15 19:27:43');
INSERT INTO `ums_admin_operation_log` VALUES (90, 1, 'admin', 'STOCK', 'STOCK_POLICY_UPDATE', '调整库存策略 sku=1，状态 ACTIVE->ACTIVE，低库存阈值 10->1，高库存阈值 1000->9999，原因=P2 smoke policy check', 'SUCCESS', '2026-06-15 19:43:34');
INSERT INTO `ums_admin_operation_log` VALUES (91, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_TASK_CREATE', '创建线上对账任务：RC20260615MOCK14414', 'SUCCESS', '2026-06-15 19:43:34');
INSERT INTO `ums_admin_operation_log` VALUES (92, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_LOCAL_BILL_GENERATE', '生成本地账单快照：taskId=7', 'SUCCESS', '2026-06-15 19:43:34');
INSERT INTO `ums_admin_operation_log` VALUES (93, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_MOCK_CHANNEL_BILL_GENERATE', '生成Mock渠道账单：taskId=7，mode=NORMAL', 'SUCCESS', '2026-06-15 19:43:34');
INSERT INTO `ums_admin_operation_log` VALUES (94, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_MATCH', '执行自动勾兑：taskId=7，差异=0', 'SUCCESS', '2026-06-15 19:43:34');
INSERT INTO `ums_admin_operation_log` VALUES (95, 1, 'admin', 'RECONCILIATION', 'ONLINE_RECONCILE_TASK_COMPLETE', '归档线上对账任务：RC20260615MOCK14414', 'SUCCESS', '2026-06-15 19:43:35');
INSERT INTO `ums_admin_operation_log` VALUES (96, 1, 'admin', 'PAY', 'PAY_SYNC', '同步订单支付状态 orderNo=ORD178152381362132B276', 'SUCCESS', '2026-06-15 19:43:35');
INSERT INTO `ums_admin_operation_log` VALUES (97, 1, 'admin', 'PAY', 'PAY_REPAIR', '补偿订单支付状态 orderNo=ORD178152381362132B276', 'SUCCESS', '2026-06-15 19:43:35');
INSERT INTO `ums_admin_operation_log` VALUES (98, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_MARK', '标记订单支付异常：ORD178152381362132B276', 'SUCCESS', '2026-06-15 19:43:35');
INSERT INTO `ums_admin_operation_log` VALUES (99, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：ORD178152381362132B276，结论=PAID_VERIFIED，建议=CONFIRM_PAID', 'SUCCESS', '2026-06-15 19:43:35');
INSERT INTO `ums_admin_operation_log` VALUES (100, 1, 'admin', 'STOCK', 'STOCK_POLICY_UPDATE', '调整库存策略 sku=1，状态 ACTIVE->ACTIVE，低库存阈值 1->1，高库存阈值 9999->9999，原因=P2 smoke policy check', 'SUCCESS', '2026-06-15 19:44:20');
INSERT INTO `ums_admin_operation_log` VALUES (101, 1, 'admin', 'PAY', 'PAY_SYNC', '同步订单支付状态 orderNo=ORD17815238604099B7C62', 'SUCCESS', '2026-06-15 19:44:21');
INSERT INTO `ums_admin_operation_log` VALUES (102, 1, 'admin', 'PAY', 'PAY_REPAIR', '补偿订单支付状态 orderNo=ORD17815238604099B7C62', 'SUCCESS', '2026-06-15 19:44:21');
INSERT INTO `ums_admin_operation_log` VALUES (103, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_MARK', '标记订单支付异常：ORD17815238604099B7C62', 'SUCCESS', '2026-06-15 19:44:21');
INSERT INTO `ums_admin_operation_log` VALUES (104, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：ORD17815238604099B7C62，结论=PAID_VERIFIED，建议=CONFIRM_PAID', 'SUCCESS', '2026-06-15 19:44:21');
INSERT INTO `ums_admin_operation_log` VALUES (105, 1, 'admin', 'STOCK', 'STOCK_POLICY_UPDATE', '调整库存策略 sku=1，状态 ACTIVE->ACTIVE，低库存阈值 1->1，高库存阈值 9999->9999，原因=P2 smoke policy check', 'SUCCESS', '2026-06-15 19:45:41');
INSERT INTO `ums_admin_operation_log` VALUES (106, 1, 'admin', 'PAY', 'PAY_SYNC', '同步订单支付状态 orderNo=ORD1781523940797C6EC98', 'SUCCESS', '2026-06-15 19:45:41');
INSERT INTO `ums_admin_operation_log` VALUES (107, 1, 'admin', 'PAY', 'PAY_REPAIR', '补偿订单支付状态 orderNo=ORD1781523940797C6EC98', 'SUCCESS', '2026-06-15 19:45:41');
INSERT INTO `ums_admin_operation_log` VALUES (108, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_MARK', '标记订单支付异常：ORD1781523940797C6EC98', 'SUCCESS', '2026-06-15 19:45:41');
INSERT INTO `ums_admin_operation_log` VALUES (109, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：ORD1781523940797C6EC98，结论=PAID_VERIFIED，建议=CONFIRM_PAID', 'SUCCESS', '2026-06-15 19:45:41');
INSERT INTO `ums_admin_operation_log` VALUES (110, 1, 'admin', 'STOCK', 'STOCK_POLICY_UPDATE', '调整库存策略 sku=1，状态 ACTIVE->ACTIVE，低库存阈值 1->1，高库存阈值 9999->9999，原因=P2 smoke policy check', 'SUCCESS', '2026-06-15 19:52:10');
INSERT INTO `ums_admin_operation_log` VALUES (111, 1, 'admin', 'PAY', 'PAY_SYNC', '同步订单支付状态 orderNo=ORD1781524329822DF7ACD', 'SUCCESS', '2026-06-15 19:52:10');
INSERT INTO `ums_admin_operation_log` VALUES (112, 1, 'admin', 'PAY', 'PAY_REPAIR', '补偿订单支付状态 orderNo=ORD1781524329822DF7ACD', 'SUCCESS', '2026-06-15 19:52:10');
INSERT INTO `ums_admin_operation_log` VALUES (113, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_MARK', '标记订单支付异常：ORD1781524329822DF7ACD', 'SUCCESS', '2026-06-15 19:52:10');
INSERT INTO `ums_admin_operation_log` VALUES (114, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：ORD1781524329822DF7ACD，结论=PAID_VERIFIED，建议=CONFIRM_PAID', 'SUCCESS', '2026-06-15 19:52:11');
INSERT INTO `ums_admin_operation_log` VALUES (115, 1, 'admin', 'STOCK', 'STOCK_POLICY_UPDATE', '调整库存策略 sku=1，状态 ACTIVE->ACTIVE，低库存阈值 1->1，高库存阈值 9999->9999，原因=P2 smoke policy check', 'SUCCESS', '2026-06-15 19:53:17');
INSERT INTO `ums_admin_operation_log` VALUES (116, 1, 'admin', 'STOCK', 'STOCK_MANUAL_ADJUST', '手工调整库存 sku=1，类型=REPLENISH，总库存 114->115，可用 114->115，锁定 0->0，原因=采购入库，备注=auto smoke', 'SUCCESS', '2026-06-15 19:53:17');
INSERT INTO `ums_admin_operation_log` VALUES (117, 1, 'admin', 'STOCK', 'STOCK_CONSISTENCY_CHECK', '发起库存一致性校验 sku=1，结果=CONSISTENT', 'SUCCESS', '2026-06-15 19:53:17');
INSERT INTO `ums_admin_operation_log` VALUES (118, 1, 'admin', 'PAY', 'PAY_SYNC', '同步订单支付状态 orderNo=ORD178152439679881F09C', 'SUCCESS', '2026-06-15 19:53:17');
INSERT INTO `ums_admin_operation_log` VALUES (119, 1, 'admin', 'PAY', 'PAY_REPAIR', '补偿订单支付状态 orderNo=ORD178152439679881F09C', 'SUCCESS', '2026-06-15 19:53:18');
INSERT INTO `ums_admin_operation_log` VALUES (120, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_MARK', '标记订单支付异常：ORD178152439679881F09C', 'SUCCESS', '2026-06-15 19:53:18');
INSERT INTO `ums_admin_operation_log` VALUES (121, 1, 'admin', 'ORDER', 'ORDER_PAYMENT_EXCEPTION_VERIFY', '核验订单支付异常：ORD178152439679881F09C，结论=PAID_VERIFIED，建议=CONFIRM_PAID', 'SUCCESS', '2026-06-15 19:53:18');
INSERT INTO `ums_admin_operation_log` VALUES (122, 1, 'admin', 'ORDER', 'ORDER_SHIP', '发货订单：ORD1781524417974BA4CFF', 'SUCCESS', '2026-06-15 19:54:04');
INSERT INTO `ums_admin_operation_log` VALUES (123, 1, 'admin', 'STOCK', 'STOCK_RECONCILIATION_IGNORE', '忽略库存对账记录 id=10，sku=12，备注=P3 smoke ignore', 'SUCCESS', '2026-06-15 20:32:02');
INSERT INTO `ums_admin_operation_log` VALUES (124, 1, 'admin', 'PRODUCT', 'PRODUCT_STATUS_UPDATE', '调整商品状态 productId=13 -> ONLINE，原因=P4 online', 'SUCCESS', '2026-06-15 20:54:57');
INSERT INTO `ums_admin_operation_log` VALUES (125, 1, 'admin', 'PRODUCT', 'PRODUCT_STATUS_UPDATE', '调整商品状态 productId=13 -> OFFLINE，原因=P4 offline', 'SUCCESS', '2026-06-15 20:54:57');
INSERT INTO `ums_admin_operation_log` VALUES (126, 1, 'admin', 'SYSTEM', 'ACCOUNT_CREATE', '创建运营账号：p4admin205458，角色=SUPER_ADMIN', 'SUCCESS', '2026-06-15 20:54:58');
INSERT INTO `ums_admin_operation_log` VALUES (127, 1, 'admin', 'SYSTEM', 'ACCOUNT_ROLE_ASSIGN', '分配运营角色：p4admin205458 -> SUPER_ADMIN', 'SUCCESS', '2026-06-15 20:54:58');
INSERT INTO `ums_admin_operation_log` VALUES (128, 1, 'admin', 'SYSTEM', 'ACCOUNT_PERMISSION_UPDATE', '更新运营账号权限：p4admin205458，角色=SUPER_ADMIN', 'SUCCESS', '2026-06-15 20:54:58');
INSERT INTO `ums_admin_operation_log` VALUES (129, 1, 'admin', 'SYSTEM', 'ACCOUNT_ENABLE', '启用运营账号：p4admin205458', 'SUCCESS', '2026-06-15 20:54:58');
INSERT INTO `ums_admin_operation_log` VALUES (130, 1, 'admin', 'ORDER', 'ORDER_SHIP', '发货订单：ORD1781528713367F731BD', 'SUCCESS', '2026-06-15 21:05:13');
INSERT INTO `ums_admin_operation_log` VALUES (131, 1, 'admin', 'ORDER', 'ORDER_COMPLETE', '完结订单：ORD1781528713367F731BD', 'SUCCESS', '2026-06-15 21:05:13');
INSERT INTO `ums_admin_operation_log` VALUES (132, 1, 'admin', 'STOCK', 'STOCK_CONSISTENCY_CHECK', '发起库存一致性校验 sku=1，结果=CONSISTENT', 'SUCCESS', '2026-06-16 00:48:40');
INSERT INTO `ums_admin_operation_log` VALUES (133, 1, 'admin', 'ORDER', 'ORDER_SHIP', '发货订单：ORD17815421181506A2B36', 'SUCCESS', '2026-06-16 00:48:41');
INSERT INTO `ums_admin_operation_log` VALUES (134, 1, 'admin', 'ORDER', 'ORDER_COMPLETE', '完结订单：ORD17815421181506A2B36', 'SUCCESS', '2026-06-16 00:48:41');
INSERT INTO `ums_admin_operation_log` VALUES (135, 1, 'admin', 'STOCK', 'STOCK_CONSISTENCY_CHECK', '发起库存一致性校验 sku=1，结果=CONSISTENT', 'SUCCESS', '2026-06-16 00:55:54');
INSERT INTO `ums_admin_operation_log` VALUES (136, 1, 'admin', 'ORDER', 'ORDER_SHIP', '发货订单：ORD17815425521398BF4BA', 'SUCCESS', '2026-06-16 00:55:55');
INSERT INTO `ums_admin_operation_log` VALUES (137, 1, 'admin', 'ORDER', 'ORDER_COMPLETE', '完结订单：ORD17815425521398BF4BA', 'SUCCESS', '2026-06-16 00:55:55');
INSERT INTO `ums_admin_operation_log` VALUES (138, 1, 'admin', 'STOCK', 'STOCK_CONSISTENCY_CHECK', '发起库存一致性校验 sku=1，结果=CONSISTENT', 'SUCCESS', '2026-06-16 20:14:36');
INSERT INTO `ums_admin_operation_log` VALUES (139, 1, 'admin', 'STOCK', 'STOCK_CONSISTENCY_CHECK', '发起库存一致性校验 sku=1，结果=CONSISTENT', 'SUCCESS', '2026-06-16 20:15:21');
INSERT INTO `ums_admin_operation_log` VALUES (140, 1, 'admin', 'PRODUCT', 'PRODUCT_STATUS_UPDATE', '调整商品状态 productId=13 -> ONLINE，原因=后台手动上架', 'SUCCESS', '2026-06-19 14:18:54');

-- ----------------------------
-- Table structure for ums_user
-- ----------------------------
DROP TABLE IF EXISTS `ums_user`;
CREATE TABLE `ums_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号',
  `password_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码哈希',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '昵称',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '头像地址',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED' COMMENT '账户状态',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ums_user_mobile`(`mobile` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ums_user
-- ----------------------------
INSERT INTO `ums_user` VALUES (1, '13800000000', '123456', '林亦辰', '/uploads/avatar/2026/06/10/3e45bbfedb234afea44177e17b15c104.png', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 02:41:25', NULL);
INSERT INTO `ums_user` VALUES (2, '13800000001', '123456', '周雨桐', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (3, '13800000002', '123456', '陈嘉明', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (4, '13800000003', '123456', '许念安', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (5, '13800000004', '123456', '赵一诺', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (6, '13800000005', '123456', '王景行', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (7, '13800000006', '123456', '李沐阳', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (8, '13800000007', '123456', '孙若溪', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (9, '13800000008', '123456', '吴泽宇', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (10, '13800000009', '123456', '郑书瑶', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (11, '13800000010', '123456', '何星河', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (12, '13800000011', '123456', '高芷晴', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (13, '13800000012', '123456', '马承宇', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (14, '13800000013', '123456', '胡安琪', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (15, '13800000014', '123456', '郭子墨', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (16, '13800000015', '123456', '罗清越', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (17, '13800000016', '123456', '梁知夏', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (18, '13800000017', '123456', '宋以航', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (19, '13800000018', '123456', '唐诗涵', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user` VALUES (20, '13800000019', '123456', '程远舟', '', 'ENABLED', 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);

-- ----------------------------
-- Table structure for ums_user_address
-- ----------------------------
DROP TABLE IF EXISTS `ums_user_address`;
CREATE TABLE `ums_user_address`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `receiver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收件人姓名',
  `receiver_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收件人手机号',
  `province_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '省编码',
  `province_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '省名称',
  `city_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '市编码',
  `city_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '市名称',
  `district_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '区编码',
  `district_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '区名称',
  `detail_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '详细地址',
  `postal_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '邮编',
  `is_default` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否默认地址',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ums_user_address_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户地址表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ums_user_address
-- ----------------------------
INSERT INTO `ums_user_address` VALUES (1, 1, '林先生', '13910002001', '110000', '北京市', '110100', '北京市', '110105', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室', '', 0, 0, '2026-01-01 09:00:00', '2026-06-10 15:42:54', NULL);
INSERT INTO `ums_user_address` VALUES (2, 2, '周女士', '13910002002', '110000', '北京市', '110100', '北京市', '110108', '海淀区', '中关村南大街甲18号院3号楼2单元501室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (3, 3, '陈先生', '13910002003', '310000', '上海市', '310100', '上海市', '310115', '浦东新区', '张江高科碧波路690号1号楼803室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (4, 4, '许女士', '13910002004', '440000', '广东省', '440300', '深圳市', '440305', '南山区', '粤海街道科技园南区深南花园6栋1802室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (5, 5, '赵女士', '13910002005', '330000', '浙江省', '330100', '杭州市', '330106', '西湖区', '文三路478号华星时代广场B座1509室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (6, 6, '王先生', '13910002006', '320000', '江苏省', '320500', '苏州市', '320508', '姑苏区', '干将西路515号佳福国际大厦908室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (7, 7, '李先生', '13910002007', '510000', '四川省', '510100', '成都市', '510107', '武侯区', '天府大道北段1700号环球中心N2区1215室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (8, 8, '孙女士', '13910002008', '420000', '湖北省', '420100', '武汉市', '420106', '武昌区', '中北路汉街总部国际E座2103室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (9, 9, '吴先生', '13910002009', '500000', '重庆市', '500100', '重庆市', '500103', '渝中区', '解放碑民族路188号环球金融中心32层3208室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (10, 10, '郑女士', '13910002010', '350000', '福建省', '350200', '厦门市', '350203', '思明区', '湖滨南路90号立信广场10楼1006室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (11, 11, '何先生', '13910002011', '370000', '山东省', '370200', '青岛市', '370202', '市南区', '香港中路61号远洋大厦A座1701室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (12, 12, '高女士', '13910002012', '120000', '天津市', '120100', '天津市', '120101', '和平区', '南京路189号津汇广场2座2205室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (13, 13, '马先生', '13910002013', '610000', '陕西省', '610100', '西安市', '610103', '碑林区', '长安北路14号省体育场东门写字楼806室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (14, 14, '胡女士', '13910002014', '430000', '湖南省', '430100', '长沙市', '430102', '芙蓉区', '五一大道766号中天广场18楼1809室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (15, 15, '郭先生', '13910002015', '210000', '辽宁省', '210100', '沈阳市', '210102', '和平区', '青年大街386号华阳国际大厦1512室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (16, 16, '罗女士', '13910002016', '410000', '河南省', '410100', '郑州市', '410105', '金水区', '农业路东16号省汇中心B座1107室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (17, 17, '梁女士', '13910002017', '340000', '安徽省', '340100', '合肥市', '340104', '蜀山区', '潜山路190号华邦ICC写字楼A座1306室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (18, 18, '宋先生', '13910002018', '230000', '黑龙江省', '230100', '哈尔滨市', '230102', '道里区', '群力第五大道金中环商业广场C座915室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (19, 19, '唐女士', '13910002019', '530000', '云南省', '530100', '昆明市', '530102', '五华区', '青年路389号志远大厦12楼1202室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (20, 20, '程先生', '13910002020', '450000', '广西壮族自治区', '450100', '南宁市', '450103', '青秀区', '民族大道136号华润大厦B座2006室', '', 1, 0, '2026-01-01 09:00:00', '2026-06-10 10:00:00', NULL);
INSERT INTO `ums_user_address` VALUES (21, 1, '艾米莉', '13910002111', '440000', '广东省', '440600', '佛山市', '440605', '南海区', '狮山镇罗村碧翠公馆', '000000', 1, 0, '2026-06-10 15:42:54', '2026-06-16 00:55:52', NULL);
INSERT INTO `ums_user_address` VALUES (22, 1, 'Auto Receiver Updated 192740', '13800138000', '110000', 'Beijing', '110100', 'Beijing', '110105', 'Chaoyang', '鑷姩鍖栨祴璇曞湴鍧€宸蹭慨鏀?192740', '100000', 0, 0, '2026-06-15 19:27:40', '2026-06-15 19:27:41', '2026-06-15 19:27:41');
INSERT INTO `ums_user_address` VALUES (23, 1, 'Auto Receiver Updated 004838', '13800138000', '110000', 'Beijing', '110100', 'Beijing', '110105', 'Chaoyang', '鑷姩鍖栨祴璇曞湴鍧€宸蹭慨鏀?004838', '100000', 0, 0, '2026-06-16 00:48:38', '2026-06-16 00:48:38', '2026-06-16 00:48:39');
INSERT INTO `ums_user_address` VALUES (24, 1, 'Auto Receiver Updated 005552', '13800138000', '110000', 'Beijing', '110100', 'Beijing', '110105', 'Chaoyang', '鑷姩鍖栨祴璇曞湴鍧€宸蹭慨鏀?005552', '100000', 0, 0, '2026-06-16 00:55:52', '2026-06-16 00:55:52', '2026-06-16 00:55:53');

-- ----------------------------
-- Table structure for ums_user_third_bind
-- ----------------------------
DROP TABLE IF EXISTS `ums_user_third_bind`;
CREATE TABLE `ums_user_third_bind`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '关联本地用户ID',
  `third_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '第三方类型',
  `third_uid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '第三方唯一标识',
  `third_nickname` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方昵称',
  `third_avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方头像',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_third_type_uid`(`third_type` ASC, `third_uid` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户第三方绑定表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ums_user_third_bind
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
