-- WMS 三期（入库闭环）：上架记录表补齐落库所需字段
--
-- 一期 V20261028_6 建 wms_warehousing_entry_up 时只留了「推荐结果 + 是否偏离」，
-- 三期真正落库还需要把「关联入库明细」和「从哪条上架任务来的」记清楚，
-- 否则上架完成后无法回溯是哪一行明细、哪一次推荐把它放进来的。
-- 全部 ADD COLUMN 前先判存在，脚本可重复执行。

SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_warehousing_entry_up` ADD COLUMN `entry_details_id` bigint NULL COMMENT ''入库明细ID'' AFTER `id`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_warehousing_entry_up' AND column_name='entry_details_id');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_warehousing_entry_up` ADD COLUMN `warehousing_entry_id` bigint NULL COMMENT ''入库单ID'' AFTER `entry_details_id`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_warehousing_entry_up' AND column_name='warehousing_entry_id');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_warehousing_entry_up` ADD COLUMN `quality_status` int NOT NULL DEFAULT 1 COMMENT ''质量状态(1良品 2待检 3不良品)'' AFTER `count`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_warehousing_entry_up' AND column_name='quality_status');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 上架记录要能按入库单反查，也要能按明细行定位
SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_warehousing_entry_up` ADD KEY `idx_up_entry` (`warehousing_entry_id`, `entry_details_id`)',
  'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='wms_warehousing_entry_up' AND index_name='idx_up_entry');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 质检单按入库单反查（一单多条明细、每条明细一张质检单）
SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_quality_check` ADD KEY `idx_qc_sku` (`sku_id`, `batch_number`)',
  'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='wms_quality_check' AND index_name='idx_qc_sku');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 入库单按状态与预计到货时间排队收货
SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_warehousing_entry` ADD KEY `idx_entry_status` (`warehouse_id`, `status`, `predicted_arrival_time`)',
  'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='wms_warehousing_entry' AND index_name='idx_entry_status');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 入库单状态注释改为三期状态机（10/20/30/40/50/60/90/99），状态值本身由字典维护
ALTER TABLE `wms_warehousing_entry`
  MODIFY COLUMN `status` int NOT NULL DEFAULT 10
    COMMENT '入库单状态(10待到货 20收货清点中 30待质检 40质检完成待上架 50上架中 60已完成 90异常挂起 99已取消)';

-- 已在用的历史单据（status 1~5）迁到新编码：1未确认→10待到货、2部分已确认→20收货中、
-- 3全部确认→30待质检、4拣货中→40待上架、5已上架→60已完成
UPDATE `wms_warehousing_entry` SET `status` = CASE `status`
    WHEN 1 THEN 10 WHEN 2 THEN 20 WHEN 3 THEN 30 WHEN 4 THEN 40 WHEN 5 THEN 60
    ELSE `status` END
WHERE `status` IN (1, 2, 3, 4, 5);

-- 入库闭环四张表的主键改成数据库自增。
-- 一期建表时沿用了「应用侧 ASSIGN_ID」的写法，只声明了 bigint 主键；
-- 三期实体改用 @TableId(type = IdType.AUTO) 让 MySQL 发号（做法同 V20261029_1 对作业单的处理），
-- 主键不自增时第一次 insert 就报 Field 'id' doesn't have a default value。
-- 四张表在三期之前从未写入过数据，直接改列不会影响存量。
SET @sql := (SELECT IF(COUNT(*)>0,
  'ALTER TABLE `wms_warehousing_entry` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID''',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_warehousing_entry' AND column_name='id'
    AND extra NOT LIKE '%auto_increment%');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(COUNT(*)>0,
  'ALTER TABLE `wms_warehousing_entry_details` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID''',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_warehousing_entry_details' AND column_name='id'
    AND extra NOT LIKE '%auto_increment%');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(COUNT(*)>0,
  'ALTER TABLE `wms_warehousing_entry_details_sort` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID''',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_warehousing_entry_details_sort' AND column_name='id'
    AND extra NOT LIKE '%auto_increment%');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(COUNT(*)>0,
  'ALTER TABLE `wms_warehousing_entry_up` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID''',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_warehousing_entry_up' AND column_name='id'
    AND extra NOT LIKE '%auto_increment%');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
