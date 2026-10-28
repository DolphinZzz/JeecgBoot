-- WMS 表结构演进 第 6 步（对应方案 4.4 顺序 6）：入库单 / 入库明细 / 上架记录扩展。
-- 补齐单号、入库类型、ERP 采购单号、MES 工单号，以及决策 D4 的超收放行状态。

ALTER TABLE `wms_warehousing_entry`
  ADD COLUMN `entry_code` varchar(100) NULL COMMENT '入库单号' AFTER `id`,
  ADD COLUMN `entry_type` int NOT NULL DEFAULT 1 COMMENT '入库类型(1采购入库 2MES生产入库 3销退入库 4调拨入库)' AFTER `entry_code`,
  ADD COLUMN `erp_po_no` varchar(100) NULL COMMENT '上游ERP采购单号' AFTER `entry_type`,
  ADD COLUMN `mes_work_order_no` varchar(100) NULL COMMENT 'MES生产工单号' AFTER `erp_po_no`,
  ADD COLUMN `over_receive_audit_status` int NOT NULL DEFAULT 0 COMMENT '超收放行状态(0无需放行 1待主管确认 2已放行 3已驳回，决策 D4)' AFTER `surpass_count`,
  ADD COLUMN `wait_supplement_flag` int NOT NULL DEFAULT 0 COMMENT '少收待补料标识(0否 1是)' AFTER `lack_count`,
  ADD UNIQUE KEY `uk_entry_code` (`entry_code`),
  ADD KEY `idx_entry_mes` (`mes_work_order_no`);

-- 单头 batch_number 与明细 batch_number 语义重叠：新逻辑以明细行为准，单头字段暂保留兼容。
ALTER TABLE `wms_warehousing_entry_details`
  ADD COLUMN `batch_number` varchar(50) NULL COMMENT '批次号' AFTER `sku_id`,
  ADD COLUMN `prod_date` date NULL COMMENT '生产日期' AFTER `batch_number`,
  ADD COLUMN `expire_date` date NULL COMMENT '有效期至' AFTER `prod_date`,
  ADD COLUMN `received_count` int NOT NULL DEFAULT 0 COMMENT '实收数量' AFTER `count`,
  ADD COLUMN `qc_status` int NOT NULL DEFAULT 1 COMMENT '质检状态(1待质检 2质检中 3合格 4不合格 5部分合格)' AFTER `status`,
  ADD COLUMN `qc_pass_count` int NOT NULL DEFAULT 0 COMMENT '质检合格数' AFTER `qc_status`,
  ADD COLUMN `qc_fail_count` int NOT NULL DEFAULT 0 COMMENT '质检不合格数' AFTER `qc_pass_count`;

-- 记录系统推荐的库位与人工偏离情况，用于度量「推荐采纳率」与固定库位溢出率。
ALTER TABLE `wms_warehousing_entry_up`
  ADD COLUMN `batch_number` varchar(50) NULL COMMENT '批次号' AFTER `sku_id`,
  ADD COLUMN `strategy_used` varchar(20) NULL COMMENT '本次使用的上架策略(FIXED/ABC/NEAREST)' AFTER `count`,
  ADD COLUMN `recommend_loca_id` int NULL COMMENT '系统推荐库位ID' AFTER `strategy_used`,
  ADD COLUMN `is_deviate` int NOT NULL DEFAULT 0 COMMENT '是否偏离推荐(1是 0否)' AFTER `recommend_loca_id`,
  ADD COLUMN `deviate_reason` varchar(255) NULL COMMENT '偏离原因' AFTER `is_deviate`,
  ADD COLUMN `is_overflow` int NOT NULL DEFAULT 0 COMMENT '是否固定库位溢出(1是 0否)' AFTER `deviate_reason`;
