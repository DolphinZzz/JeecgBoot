-- WMS 二期修正：盘点记录刚生成时尚未录入实盘数，correct_count 必须允许为空。
-- 原表定义为 NOT NULL 且无默认值，导致「生成盘点任务」写库失败（Field 'correct_count' doesn't have a default value）。

ALTER TABLE `wms_stocktaking_info`
  MODIFY COLUMN `correct_count` int NULL COMMENT '实盘数量(未录入时为空)';
