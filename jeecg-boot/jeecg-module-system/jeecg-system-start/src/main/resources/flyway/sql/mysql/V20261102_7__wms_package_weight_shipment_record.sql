-- 完善出库包装箱单、称重记录与物流发运全流程跟踪字段
ALTER TABLE `wms_outbound_order_package`
  ADD COLUMN `package_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '纸箱' COMMENT '包装类型' AFTER `package_code`;

ALTER TABLE `wms_outbound_order_weights`
  ADD COLUMN `package_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联箱号' AFTER `weights_code`,
  ADD COLUMN `package_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '纸箱' COMMENT '包装类型' AFTER `package_code`,
  ADD COLUMN `package_count` int DEFAULT 0 COMMENT '装箱件数' AFTER `package_type`,
  ADD COLUMN `actual_weight` double DEFAULT 0 COMMENT '实测重量(kg)' AFTER `weight_val`,
  ADD COLUMN `theoretical_weight` double DEFAULT 0 COMMENT '理论重量(kg)' AFTER `actual_weight`,
  ADD COLUMN `diff_weight` double DEFAULT 0 COMMENT '重量偏差(kg)' AFTER `theoretical_weight`;

ALTER TABLE `wms_outbound_order`
  ADD COLUMN `freight_fee` decimal(10,2) DEFAULT 0.00 COMMENT '物流运费(元)' AFTER `logistics_number`,
  ADD COLUMN `ship_time` datetime DEFAULT NULL COMMENT '发运出库时间' AFTER `freight_fee`,
  ADD COLUMN `ship_by` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发货确认人' AFTER `ship_time`;

