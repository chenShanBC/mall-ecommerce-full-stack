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

 Date: 24/06/2026 15:46:00
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
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '后台运营轻量配置表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 87 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '认证会话安全事件日志表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存锁记录表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 107 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存操作日志表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存对账记录表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 33 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单项表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付回调记录表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '支付单表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品类目表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品每日完成销量统计表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品销售统计事件幂等表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品SKU表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 168 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '运营后台操作日志表' ROW_FORMAT = DYNAMIC;

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户第三方绑定表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
