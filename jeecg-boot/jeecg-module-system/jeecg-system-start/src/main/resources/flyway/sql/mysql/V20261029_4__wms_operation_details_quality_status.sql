-- WMS 二期修正：库内作业单明细缺少质量状态列。
-- 库存唯一键 uk_inv_dim 是「库位 + SKU + 批次 + 货主 + 质量状态」五维，
-- 移库/补货定位库存行必须带质量状态，缺少该列会导致明细写入报 Unknown column 'quality_status'。

ALTER TABLE `wms_warehouse_operations_details`
  ADD COLUMN `quality_status` int NOT NULL DEFAULT 1 COMMENT '质量状态(1良品 2待检 3不良品)' AFTER `consignor_id`;
