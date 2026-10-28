-- WMS 表结构演进 第 2 步（对应方案 4.4 顺序 2）：SPU / SKU 作业属性扩展。
-- SPU 承载存储条件、贵品标记、效期规则；SKU 承载质检方式、SN 开关、补货阈值、上架策略。

-- abc_class 为「初始 / 兜底」等级：实际生效等级按仓库+SKU 存在 wms_abc_class_result。
ALTER TABLE `wms_item_spu`
  ADD COLUMN `abc_class` varchar(2) NULL COMMENT '初始/兜底ABC等级(实际生效等级见 wms_abc_class_result)' AFTER `supplier_id`,
  ADD COLUMN `storage_condition` varchar(30) NULL COMMENT '存储条件(字典 wms_storage_condition)' AFTER `abc_class`,
  ADD COLUMN `is_valuable` int NOT NULL DEFAULT 0 COMMENT '是否贵品(1是 0否)，用于分区推导' AFTER `storage_condition`,
  ADD COLUMN `is_shelf_life` int NOT NULL DEFAULT 0 COMMENT '是否效期管理(1是 0否)' AFTER `is_valuable`,
  ADD COLUMN `shelf_life_days` int NULL COMMENT '保质期天数' AFTER `is_shelf_life`,
  ADD COLUMN `near_expire_days` int NULL COMMENT '临期禁出阈值天数（决策 D5：按商品配置，仓库不设全局值）' AFTER `shelf_life_days`;

-- 决策 D2：SN 管理按商品开关；决策 D5：临期禁出阈值挂在 SPU 上。
ALTER TABLE `wms_item_sku`
  ADD COLUMN `qc_type` int NOT NULL DEFAULT 2 COMMENT '默认质检方式(1免检 2抽检 3全检)' AFTER `code`,
  ADD COLUMN `is_sn_managed` int NOT NULL DEFAULT 0 COMMENT '是否SN管理(1是 0否，决策 D2：按商品开关)' AFTER `qc_type`,
  ADD COLUMN `min_stock` int NOT NULL DEFAULT 0 COMMENT '拣货区补货触发下限' AFTER `is_sn_managed`,
  ADD COLUMN `max_stock` int NOT NULL DEFAULT 0 COMMENT '拣货区最大安全库存' AFTER `min_stock`,
  ADD COLUMN `putaway_strategy` varchar(20) NULL COMMENT '上架策略(FIXED固定 ABC驱动 NEAREST就近)，为空用仓库默认' AFTER `max_stock`;
