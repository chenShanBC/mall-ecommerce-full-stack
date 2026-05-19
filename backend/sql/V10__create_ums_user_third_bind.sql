CREATE TABLE IF NOT EXISTS `ums_user_third_bind` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '关联本地用户ID',
  `third_type` VARCHAR(32) NOT NULL COMMENT '第三方类型',
  `third_uid` VARCHAR(128) NOT NULL COMMENT '第三方唯一标识',
  `third_nickname` VARCHAR(128) DEFAULT NULL COMMENT '第三方昵称',
  `third_avatar` VARCHAR(512) DEFAULT NULL COMMENT '第三方头像',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_third_type_uid` (`third_type`, `third_uid`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户第三方绑定表';
