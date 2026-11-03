-- ============================================================================
-- 库内作业单支持跨仓库移动（调出仓库 -> 调入仓库）
-- 1. 为 wms_warehouse_operations 补充 target_warehouse_id
-- 2. 存量作业单回填 target_warehouse_id = warehouse_id
-- ============================================================================

ALTER TABLE `wms_warehouse_operations`
  ADD COLUMN `target_warehouse_id` int NULL COMMENT '目标仓库ID(调入仓库)' AFTER `warehouse_id`;

UPDATE `wms_warehouse_operations`
  SET `target_warehouse_id` = `warehouse_id`
  WHERE `target_warehouse_id` IS NULL;
