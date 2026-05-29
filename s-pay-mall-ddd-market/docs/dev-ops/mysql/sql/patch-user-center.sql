-- =============================================================================
-- 个人中心增量：地址 / 收藏 / 浏览 + pay_order 收货快照
-- 库：s-pay-mall-ddd-market（与 s-pay-mall-ddd-market.sql 配套）
-- 可重复执行：表用 IF NOT EXISTS；pay_order 列用 information_schema 判断
-- =============================================================================

USE `s-pay-mall-ddd-market`;

CREATE TABLE IF NOT EXISTS `user_address` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `receiver_name` varchar(32) NOT NULL COMMENT '收货人',
  `receiver_phone` varchar(20) NOT NULL COMMENT '手机号',
  `province` varchar(32) NOT NULL COMMENT '省',
  `city` varchar(32) NOT NULL COMMENT '市',
  `district` varchar(32) NOT NULL COMMENT '区',
  `detail_address` varchar(200) NOT NULL COMMENT '详细地址',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认 0否 1是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址';

CREATE TABLE IF NOT EXISTS `user_favorite` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` varchar(64) NOT NULL,
  `goods_id` varchar(16) NOT NULL COMMENT '商品ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_goods` (`user_id`,`goods_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏';

CREATE TABLE IF NOT EXISTS `user_browse_history` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` varchar(64) NOT NULL,
  `goods_id` varchar(16) NOT NULL,
  `browse_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_goods` (`user_id`,`goods_id`),
  KEY `idx_user_browse` (`user_id`,`browse_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户浏览记录';

-- pay_order 地址快照列（幂等添加）
SET @db = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'pay_order' AND COLUMN_NAME = 'address_id') = 0,
  'ALTER TABLE `pay_order` ADD COLUMN `address_id` bigint(20) unsigned DEFAULT NULL COMMENT ''地址ID'' AFTER `pay_amount`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'pay_order' AND COLUMN_NAME = 'receiver_name') = 0,
  'ALTER TABLE `pay_order` ADD COLUMN `receiver_name` varchar(32) DEFAULT NULL COMMENT ''收货人快照'' AFTER `address_id`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'pay_order' AND COLUMN_NAME = 'receiver_phone') = 0,
  'ALTER TABLE `pay_order` ADD COLUMN `receiver_phone` varchar(20) DEFAULT NULL COMMENT ''手机快照'' AFTER `receiver_name`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'pay_order' AND COLUMN_NAME = 'receiver_region') = 0,
  'ALTER TABLE `pay_order` ADD COLUMN `receiver_region` varchar(128) DEFAULT NULL COMMENT ''省市区快照'' AFTER `receiver_phone`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'pay_order' AND COLUMN_NAME = 'receiver_detail') = 0,
  'ALTER TABLE `pay_order` ADD COLUMN `receiver_detail` varchar(200) DEFAULT NULL COMMENT ''详细地址快照'' AFTER `receiver_region`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
