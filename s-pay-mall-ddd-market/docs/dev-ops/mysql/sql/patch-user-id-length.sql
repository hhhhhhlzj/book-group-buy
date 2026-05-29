-- 扩展 user_id 长度（微信 openid / 指纹；并防止历史误写入 ticket）
USE `s-pay-mall-ddd-market`;

ALTER TABLE `pay_order` MODIFY COLUMN `user_id` varchar(64) NOT NULL COMMENT '用户ID';
ALTER TABLE `user_address` MODIFY COLUMN `user_id` varchar(64) NOT NULL COMMENT '用户ID';
ALTER TABLE `user_favorite` MODIFY COLUMN `user_id` varchar(64) NOT NULL;
ALTER TABLE `user_browse_history` MODIFY COLUMN `user_id` varchar(64) NOT NULL;
