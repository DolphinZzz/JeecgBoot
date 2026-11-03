-- WMS 库位商品规划与容量设置表
-- 在仓储管理建完仓库-库区-库位后，作业人员在库存管理中设置库位可存放的商品(SPU)与最大存放件数
-- 混合库位同一个库位最多允许配置不超过 3 个 SPU

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `wms_loca_item_capacity` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `warehouse_id` int NOT NULL COMMENT '所属仓库ID',
  `warehouse_area_id` int NOT NULL COMMENT '所属库区ID',
  `warehouse_area_loca_id` int NOT NULL COMMENT '库位ID',
  `spu_id` bigint NOT NULL COMMENT '商品SPU ID',
  `max_count` int NOT NULL DEFAULT 0 COMMENT '最大可存放件数',
  `remark` varchar(255) NULL DEFAULT NULL COMMENT '配置备注',
  `create_by` varchar(32) NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `is_del` int NOT NULL DEFAULT 1 COMMENT '是否删除 1未删除 2已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_loca_spu` (`warehouse_area_loca_id`, `spu_id`, `is_del`),
  KEY `idx_wh_loca` (`warehouse_id`, `warehouse_area_loca_id`),
  KEY `idx_spu` (`spu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库位商品规划与容量设置';
