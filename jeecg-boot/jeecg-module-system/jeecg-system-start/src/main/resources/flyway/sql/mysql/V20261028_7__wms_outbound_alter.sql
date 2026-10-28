-- WMS 表结构演进 第 7 步（对应方案 4.4 顺序 7）：出库单 / 出库明细扩展。
-- 补齐单号、出库类型、拣货模式、波次、ERP 单号、MES 工单号，以及复核与缺货处理字段。

ALTER TABLE `wms_outbound_order`
  ADD COLUMN `outbound_code` varchar(100) NULL COMMENT '出库单号' AFTER `id`,
  ADD COLUMN `outbound_type` int NOT NULL DEFAULT 1 COMMENT '出库类型(1销售出库 2MES工单领料 3采购退货 4调拨出库)' AFTER `outbound_code`,
  ADD COLUMN `pick_mode` int NOT NULL DEFAULT 1 COMMENT '拣货模式(1摘果法 2波次播种法)' AFTER `outbound_type`,
  ADD COLUMN `wave_id` bigint NULL COMMENT '波次ID' AFTER `pick_mode`,
  ADD COLUMN `erp_order_no` varchar(100) NULL COMMENT '上游ERP单号' AFTER `wave_id`,
  ADD COLUMN `mes_work_order_no` varchar(100) NULL COMMENT 'MES生产工单号' AFTER `erp_order_no`,
  ADD UNIQUE KEY `uk_outbound_code` (`outbound_code`),
  ADD KEY `idx_outbound_wave` (`wave_id`),
  ADD KEY `idx_outbound_mes` (`mes_work_order_no`);

-- target_warehouse_* 三列命名源自调拨场景（目标仓），销售出库时语义实为「分配库位」；
-- 本期不改列名，仅在新代码的实体注释中说明。
ALTER TABLE `wms_outbound_order_details`
  ADD COLUMN `batch_number` varchar(50) NULL COMMENT '分配批次号' AFTER `sku_id`,
  ADD COLUMN `expire_date` date NULL COMMENT '有效期至' AFTER `batch_number`,
  ADD COLUMN `allocated_count` int NOT NULL DEFAULT 0 COMMENT '已分配数量' AFTER `out_count`,
  ADD COLUMN `picked_count` int NOT NULL DEFAULT 0 COMMENT '实拣数量' AFTER `allocated_count`,
  ADD COLUMN `review_count` int NOT NULL DEFAULT 0 COMMENT '复核数量' AFTER `picked_count`,
  ADD COLUMN `review_status` int NOT NULL DEFAULT 0 COMMENT '复核状态(0未复核 1通过 2不通过)' AFTER `review_count`,
  ADD COLUMN `shortage_handle` int NOT NULL DEFAULT 1 COMMENT '缺货处理(1剔除缺货行 2整单挂起)' AFTER `review_status`;
