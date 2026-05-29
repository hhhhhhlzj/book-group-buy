-- =============================================================================
-- 多商品（书）增量脚本：在已有 group_buy_market 库上执行（与 2-29-group_buy_market.sql 配套）
-- 执行前请备份。可重复执行：依赖 sku.goods_id / group_buy_activity.activity_id /
-- sc_sku_activity(source,channel,goods_id) 唯一约束做 UPSERT。
-- =============================================================================

USE `group_buy_market`;

-- 新书 SKU（原价用于试算；拼团直减金额由折扣 25120207 规则决定）
INSERT INTO `sku` (`source`, `channel`, `goods_id`, `goods_name`, `original_price`, `create_time`, `update_time`)
VALUES
  ('s01','c01','9890002','《Spring Boot 实战》',89.00,NOW(),NOW()),
  ('s01','c01','9890003','《Redis 设计与实现》',79.00,NOW(),NOW()),
  ('s01','c01','9890004','《深入理解 Java 虚拟机》',119.00,NOW(),NOW()),
  ('s01','c01','9890005','《RabbitMQ 实战指南》',69.00,NOW(),NOW())
ON DUPLICATE KEY UPDATE
  `goods_name` = VALUES(`goods_name`),
  `original_price` = VALUES(`original_price`),
  `update_time` = NOW();

-- 每书独立活动，避免 MarketIndex 按 activityId 拉取拼团队伍时串单
INSERT INTO `group_buy_activity` (`activity_id`, `activity_name`, `discount_id`, `group_type`, `take_limit_count`, `target`, `valid_time`, `status`, `start_time`, `end_time`, `tag_id`, `tag_scope`, `create_time`, `update_time`)
VALUES
  (100124,'书香拼团-Spring Boot','25120207',0,1,3,15,1,'2024-12-07 10:19:40','2029-12-07 10:19:40','1','1',NOW(),NOW()),
  (100125,'书香拼团-Redis','25120207',0,1,3,15,1,'2024-12-07 10:19:40','2029-12-07 10:19:40','1','1',NOW(),NOW()),
  (100126,'书香拼团-JVM','25120207',0,1,3,15,1,'2024-12-07 10:19:40','2029-12-07 10:19:40','1','1',NOW(),NOW()),
  (100127,'书香拼团-RabbitMQ','25120207',0,1,3,15,1,'2024-12-07 10:19:40','2029-12-07 10:19:40','1','1',NOW(),NOW())
ON DUPLICATE KEY UPDATE
  `activity_name` = VALUES(`activity_name`),
  `discount_id` = VALUES(`discount_id`),
  `update_time` = NOW();

-- 渠道商品与活动映射（试算通过 goodsId 解析 activityId）
INSERT INTO `sc_sku_activity` (`source`, `channel`, `activity_id`, `goods_id`, `create_time`, `update_time`)
VALUES
  ('s01','c01',100124,'9890002',NOW(),NOW()),
  ('s01','c01',100125,'9890003',NOW(),NOW()),
  ('s01','c01',100126,'9890004',NOW(),NOW()),
  ('s01','c01',100127,'9890005',NOW(),NOW())
ON DUPLICATE KEY UPDATE
  `activity_id` = VALUES(`activity_id`),
  `update_time` = NOW();
