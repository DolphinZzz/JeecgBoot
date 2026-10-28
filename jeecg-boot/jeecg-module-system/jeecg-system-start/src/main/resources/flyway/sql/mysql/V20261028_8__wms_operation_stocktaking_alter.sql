-- WMS 表结构演进 第 8 步（对应方案 4.4 顺序 8）：库内作业 / 盘点 / 报损扩展。
-- 移库、补货等作业需要区分来源（人工 / ABC 优化建议 / 系统补货）与来源批次；
-- 盘点补货主与批次维度，差异审批状态承担调账留痕。

ALTER TABLE `wms_warehouse_operations`
  ADD COLUMN `operation_type` int NOT NULL DEFAULT 1 COMMENT '作业子类(1移库移位 2补货 3加工 4取消返架)' AFTER `category`,
  ADD COLUMN `source_type` int NOT NULL DEFAULT 1 COMMENT '来源(1人工创建 2ABC优化建议 3系统补货)' AFTER `operation_type`,
  ADD COLUMN `source_batch_id` bigint NULL COMMENT '来源ABC计算批次ID' AFTER `source_type`;

ALTER TABLE `wms_warehouse_operations_details`
  ADD COLUMN `batch_number` varchar(50) NULL COMMENT '批次号' AFTER `sku_id`,
  ADD COLUMN `consignor_id` bigint NULL COMMENT '货主ID' AFTER `batch_number`,
  ADD COLUMN `source_warehouse_area_id` int NULL COMMENT '来源库区ID' AFTER `target_count`,
  ADD COLUMN `source_warehouse_area_loca_id` int NULL COMMENT '来源库位ID' AFTER `source_warehouse_area_id`;

ALTER TABLE `wms_stocktaking_info`
  ADD COLUMN `consignor_id` bigint NULL COMMENT '货主ID' AFTER `sku_id`,
  ADD COLUMN `batch_number` varchar(50) NULL COMMENT '批次号' AFTER `consignor_id`,
  ADD COLUMN `expire_date` date NULL COMMENT '有效期至' AFTER `batch_number`,
  ADD COLUMN `audit_status` int NOT NULL DEFAULT 0 COMMENT '差异审批状态(0无差异 1待审批 2已通过 3已驳回)' AFTER `discrepancy_count`,
  ADD COLUMN `audit_by` varchar(30) NULL COMMENT '审批人' AFTER `audit_status`,
  ADD COLUMN `audit_time` datetime NULL COMMENT '审批时间' AFTER `audit_by`;

-- 现有列 stocktaking_info 语义为「盘点信息ID」，与表名重名；重命名需同步改代码，本期保留。
ALTER TABLE `wms_stocktaking_loss`
  ADD COLUMN `loss_type` int NOT NULL DEFAULT 1 COMMENT '损溢类型(1盘亏 2盘盈)' AFTER `loss_category`,
  ADD COLUMN `batch_number` varchar(50) NULL COMMENT '批次号' AFTER `stocktaking_info`;
