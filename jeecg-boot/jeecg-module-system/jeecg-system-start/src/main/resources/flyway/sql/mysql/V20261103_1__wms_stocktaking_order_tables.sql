-- WMS 盘库单主子表结构与白名单
-- 支持主表单据化管理（单号、仓库、单据日期、实际金额、备注、状态）
-- 支持子表明细（规格编号、规格名、商品名、账面库存、单价、实际库存、实际金额、盈亏数、实际盈亏金额）

CREATE TABLE IF NOT EXISTS `wms_stocktaking_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(50) NOT NULL COMMENT '盘库单号',
  `warehouse_id` int NOT NULL COMMENT '仓库ID',
  `order_date` date NOT NULL COMMENT '单据日期',
  `total_actual_amount` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '实际总金额',
  `total_book_count` int NOT NULL DEFAULT 0 COMMENT '账面总数量',
  `total_actual_count` int NOT NULL DEFAULT 0 COMMENT '实际总数量',
  `total_discrepancy_count` int NOT NULL DEFAULT 0 COMMENT '盈亏总数量',
  `total_discrepancy_amount` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '实际盈亏总金额',
  `remark` varchar(255) NULL COMMENT '备注',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态(0草稿/暂存 1已完成 2待审批 3已驳回)',
  `approval_switch` int NOT NULL DEFAULT 0 COMMENT '审批开关(0关闭 1开启)',
  `sys_org_code` varchar(64) NULL COMMENT '所属部门',
  `create_by` varchar(32) NULL COMMENT '创建人',
  `create_time` datetime NULL COMMENT '创建日期',
  `update_by` varchar(32) NULL COMMENT '更新人',
  `update_time` datetime NULL COMMENT '更新日期',
  `is_del` int NOT NULL DEFAULT 1 COMMENT '是否删除(1未删除 2已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stk_order_no` (`order_no`),
  KEY `idx_stk_order_wh_status` (`warehouse_id`, `status`),
  KEY `idx_stk_order_date` (`order_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘库单主表';

CREATE TABLE IF NOT EXISTS `wms_stocktaking_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` bigint NOT NULL COMMENT '盘库单ID',
  `order_no` varchar(50) NOT NULL COMMENT '盘库单号',
  `sku_id` bigint NOT NULL COMMENT 'SKU规格ID',
  `sku_code` varchar(50) NULL COMMENT '规格编号',
  `sku_name` varchar(100) NULL COMMENT '规格名',
  `spu_name` varchar(100) NULL COMMENT '商品名',
  `unit` varchar(20) NULL COMMENT '单位',
  `book_count` int NOT NULL DEFAULT 0 COMMENT '账面库存',
  `unit_price` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '单价(元)',
  `actual_count` int NULL COMMENT '实际库存(件/个)',
  `actual_amount` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '实际金额(元)',
  `discrepancy_count` int NOT NULL DEFAULT 0 COMMENT '盈亏数',
  `discrepancy_amount` decimal(15,2) NOT NULL DEFAULT 0.00 COMMENT '实际盈亏金额(元)',
  `create_by` varchar(32) NULL COMMENT '创建人',
  `create_time` datetime NULL COMMENT '创建日期',
  `update_by` varchar(32) NULL COMMENT '更新人',
  `update_time` datetime NULL COMMENT '更新日期',
  `is_del` int NOT NULL DEFAULT 1 COMMENT '是否删除(1未删除 2已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_stk_dtl_order` (`order_id`),
  KEY `idx_stk_dtl_sku` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘库单明细表';

-- 表字典白名单
INSERT INTO sys_table_white_list(id, table_name, field_name, status, create_by, create_time)
SELECT '179360000000001', 'wms_stocktaking_order', 'id,order_no,warehouse_id,order_date,status,is_del', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_table_white_list WHERE table_name = 'wms_stocktaking_order');

INSERT INTO sys_table_white_list(id, table_name, field_name, status, create_by, create_time)
SELECT '179360000000002', 'wms_stocktaking_detail', 'id,order_id,order_no,sku_id,sku_code,is_del', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_table_white_list WHERE table_name = 'wms_stocktaking_detail');
