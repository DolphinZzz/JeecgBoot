-- ============================================================================
-- 修复出库单及子表 is_del 默认值与历史 NULL 数据
-- 1. 存量记录中 is_del IS NULL 的数据刷回 1（未删除）
-- 2. 将 wms_outbound_order / wms_outbound_order_details / wms_outbound_order_package / wms_outbound_order_weights
--    的 is_del 字段设置为 NOT NULL DEFAULT 1
-- ============================================================================

-- 1. 存量历史数据修正
UPDATE `wms_outbound_order` SET `is_del` = 1 WHERE `is_del` IS NULL;
UPDATE `wms_outbound_order_details` SET `is_del` = 1 WHERE `is_del` IS NULL;
UPDATE `wms_outbound_order_package` SET `is_del` = 1 WHERE `is_del` IS NULL;
UPDATE `wms_outbound_order_weights` SET `is_del` = 1 WHERE `is_del` IS NULL;

-- 2. 字段约束调整：默认值设为 1
ALTER TABLE `wms_outbound_order`
  MODIFY COLUMN `is_del` int NOT NULL DEFAULT 1 COMMENT '是否删除 1未删除 2已删除';

ALTER TABLE `wms_outbound_order_details`
  MODIFY COLUMN `is_del` int NOT NULL DEFAULT 1 COMMENT '是否删除 1未删除 2已删除';

ALTER TABLE `wms_outbound_order_package`
  MODIFY COLUMN `is_del` int NOT NULL DEFAULT 1 COMMENT '是否删除 1未删除 2已删除';

ALTER TABLE `wms_outbound_order_weights`
  MODIFY COLUMN `is_del` int NOT NULL DEFAULT 1 COMMENT '是否删除 1未删除 2已删除';
