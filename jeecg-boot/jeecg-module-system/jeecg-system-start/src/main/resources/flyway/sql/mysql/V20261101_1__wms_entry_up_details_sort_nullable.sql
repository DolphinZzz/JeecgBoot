-- WMS 上架记录表分拣明细ID改为可空
-- 质检不合格品退货处置 / 破损件落库直接分流至退货不良区，不强制依赖分拣明细ID
SET NAMES utf8mb4;

SET @sql := (SELECT IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'wms_warehousing_entry_up'
      AND column_name = 'details_sort_id'
      AND is_nullable = 'NO'
  ),
  'ALTER TABLE `wms_warehousing_entry_up` MODIFY COLUMN `details_sort_id` bigint NULL DEFAULT NULL COMMENT ''分拣明细ID''',
  'SELECT 1'
));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
