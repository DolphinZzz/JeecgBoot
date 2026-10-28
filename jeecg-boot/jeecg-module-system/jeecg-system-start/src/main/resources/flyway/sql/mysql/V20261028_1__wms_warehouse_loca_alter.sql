-- WMS 表结构演进 第 1 步（对应方案 4.4 顺序 1）：库位 / 库区 / 仓库扩展。
-- 库位补齐仓库与库区归属（原表缺这两列，无法按库区维度查询与上架推荐）；
-- 补充上架策略所需的分区、距离、人体工学、固定绑定等字段。

ALTER TABLE `wms_warehouse_area_loca`
  ADD COLUMN `warehouse_id` int NULL COMMENT '所属仓库ID' AFTER `id`,
  ADD COLUMN `warehouse_area_id` int NULL COMMENT '所属库区ID' AFTER `warehouse_id`,
  ADD COLUMN `abc_class` varchar(2) NULL COMMENT 'ABC分类 A/B/C' AFTER `layer_number`,
  ADD COLUMN `storage_condition` varchar(30) NULL COMMENT '存储条件(字典 wms_storage_condition)' AFTER `abc_class`,
  ADD COLUMN `loca_status` int NOT NULL DEFAULT 1 COMMENT '库位状态(1空闲 2占用 3盘点锁定 4禁用)' AFTER `storage_condition`,
  ADD COLUMN `allow_mix_sku` int NOT NULL DEFAULT 0 COMMENT '是否允许混SKU(1是 0否)' AFTER `loca_status`,
  ADD COLUMN `allow_mix_batch` int NOT NULL DEFAULT 1 COMMENT '是否允许混批次(1是 0否)' AFTER `allow_mix_sku`,
  ADD COLUMN `used_volume` int NOT NULL DEFAULT 0 COMMENT '已用体积' AFTER `allow_mix_batch`,
  ADD COLUMN `used_weight` int NOT NULL DEFAULT 0 COMMENT '已用承重' AFTER `used_volume`,
  ADD COLUMN `pick_distance` int NULL COMMENT '到拣货/复核区路径距离(米)，用于库位得分' AFTER `used_weight`,
  ADD COLUMN `receive_distance` int NULL COMMENT '到收货区路径距离(米)，用于就近上架' AFTER `pick_distance`,
  ADD COLUMN `ergonomic_level` int NOT NULL DEFAULT 2 COMMENT '人体工学层级(1黄金层 2次优层 3不便层)' AFTER `receive_distance`,
  ADD COLUMN `loca_score` decimal(6,4) NULL COMMENT '库位得分(越高越好取)，由库位等级计算回写' AFTER `ergonomic_level`,
  ADD COLUMN `fixed_sku_id` bigint NULL COMMENT '固定库位绑定的SKU，为空表示非固定库位' AFTER `loca_score`,
  ADD KEY `idx_loca_wh_area` (`warehouse_id`,`warehouse_area_id`),
  ADD KEY `idx_loca_status` (`loca_status`),
  ADD KEY `idx_loca_fixed_sku` (`fixed_sku_id`);

ALTER TABLE `wms_warehouse_area`
  ADD COLUMN `storage_condition` varchar(30) NULL COMMENT '存储条件(字典 wms_storage_condition)' AFTER `category`,
  ADD COLUMN `abc_class` varchar(2) NULL COMMENT 'ABC分类 A/B/C' AFTER `storage_condition`,
  ADD COLUMN `zone_type` varchar(30) NOT NULL DEFAULT 'GENERAL' COMMENT '分区类型(字典 wms_zone_type：GENERAL通用 SMALL小件 LARGE大件 VALUABLE贵品)' AFTER `abc_class`;

-- 决策 D1：工业仓与普通仓共存，仓型以字典 wms_warehouse_type 承载（NORMAL / INDUSTRIAL），不使用整数编码。
ALTER TABLE `wms_warehouse`
  ADD COLUMN `warehouse_type` varchar(30) NOT NULL DEFAULT 'NORMAL' COMMENT '仓库类型(字典 wms_warehouse_type：NORMAL 普通仓 / INDUSTRIAL 工业制造仓)' AFTER `name`,
  ADD COLUMN `manager_phone` varchar(20) NULL COMMENT '负责人电话' AFTER `person_charge_id`,
  ADD COLUMN `enable_status` int NOT NULL DEFAULT 1 COMMENT '启用状态(1启用 0停用)' AFTER `sort`;
