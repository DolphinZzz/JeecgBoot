-- 修复库内作业明细表 wms_warehouse_operations_details 的 category 字段默认值，防止取消出库返架等写入明细时触发非空完整性异常
ALTER TABLE `wms_warehouse_operations_details`
  MODIFY COLUMN `category` int NOT NULL DEFAULT 1 COMMENT '类型(1来源 2目标)';
