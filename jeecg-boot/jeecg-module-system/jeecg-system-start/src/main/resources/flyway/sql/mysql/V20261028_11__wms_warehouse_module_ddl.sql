-- WMS 仓储管理基础模块表结构调整
-- 1. 仓库表：id自增，负责人字段改为varchar(32)存sys_user.id，省市区与地址允许为空
-- 2. 库区表：id自增，is_del 默认 1
-- 3. 库位表：id自增，尺寸容积等字段补充默认值0，巷道货架层号补充默认空串，is_del 默认 1

ALTER TABLE `wms_warehouse`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `person_charge_id` varchar(32) NULL COMMENT '负责人用户ID(sys_user.id)',
  MODIFY COLUMN `person_charge_name` varchar(30) NULL COMMENT '负责人姓名',
  MODIFY COLUMN `city_id` int NULL COMMENT '所在城市ID',
  MODIFY COLUMN `city_name` varchar(100) NULL COMMENT '所在城市名称',
  MODIFY COLUMN `province_id` int NULL COMMENT '所在省份ID',
  MODIFY COLUMN `province_name` varchar(100) NULL COMMENT '所在省份名称',
  MODIFY COLUMN `address` varchar(300) NULL COMMENT '详细地址',
  MODIFY COLUMN `is_del` int NOT NULL DEFAULT 1 COMMENT '是否删除 1未删除 2已删除';

ALTER TABLE `wms_warehouse_area`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `is_del` int NOT NULL DEFAULT 1 COMMENT '是否删除 1未删除 2已删除';

ALTER TABLE `wms_warehouse_area_loca`
  MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `loca_len` int NOT NULL DEFAULT 0 COMMENT '库位长度',
  MODIFY COLUMN `loca_width` int NOT NULL DEFAULT 0 COMMENT '库位宽度',
  MODIFY COLUMN `loca_height` int NOT NULL DEFAULT 0 COMMENT '库位高度',
  MODIFY COLUMN `local_volume` int NOT NULL DEFAULT 0 COMMENT '库位容积',
  MODIFY COLUMN `local_bearing_capacity` int NOT NULL DEFAULT 0 COMMENT '库位承重',
  MODIFY COLUMN `tunnel_number` varchar(30) NOT NULL DEFAULT '' COMMENT '巷道号',
  MODIFY COLUMN `shelf_number` varchar(30) NOT NULL DEFAULT '' COMMENT '货架号',
  MODIFY COLUMN `layer_number` varchar(30) NOT NULL DEFAULT '' COMMENT '层号',
  MODIFY COLUMN `is_del` int NOT NULL DEFAULT 1 COMMENT '是否删除 1未删除 2已删除';
