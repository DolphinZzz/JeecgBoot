-- WMS 表结构演进 第 4 步（对应方案 4.4 顺序 4）：质检单与出库波次。
-- 质检单承载「收货 → 质检 → 上架」中的质检环节，并记录绑定的 MES 生产工单；
-- 波次表按决策 D3 以「承运商 + 温区」分波。

CREATE TABLE IF NOT EXISTS `wms_quality_check` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `qc_code` varchar(100) NOT NULL COMMENT '质检单号',
  `warehousing_entry_id` bigint NOT NULL COMMENT '入库单ID',
  `entry_details_id` bigint NOT NULL COMMENT '入库明细ID',
  `mes_work_order_no` varchar(100) DEFAULT NULL COMMENT 'MES生产工单号',
  `sku_id` bigint NOT NULL COMMENT '规格ID',
  `batch_number` varchar(50) DEFAULT NULL COMMENT '批次号',
  `qc_type` int NOT NULL DEFAULT 2 COMMENT '质检方式(1免检 2抽检 3全检)',
  `check_count` int NOT NULL DEFAULT 0 COMMENT '送检数量',
  `sample_count` int NOT NULL DEFAULT 0 COMMENT '抽检数量',
  `pass_count` int NOT NULL DEFAULT 0 COMMENT '合格数量',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '不合格数量',
  `fail_reason` varchar(255) DEFAULT NULL COMMENT '不合格原因',
  `return_order_no` varchar(100) DEFAULT NULL COMMENT '关联退货单号',
  `qc_emp_id` int DEFAULT NULL COMMENT '质检员工ID',
  `qc_time` datetime DEFAULT NULL COMMENT '质检时间',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态(1待质检 2质检中 3合格 4不合格 5部分合格)',
  `saas_id` varchar(60) DEFAULT NULL COMMENT 'SaaS标识',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建人账号',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '修改人账号',
  `is_del` int DEFAULT 1 COMMENT '是否删除 1未删除 2已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qc_code` (`qc_code`),
  KEY `idx_qc_entry` (`warehousing_entry_id`,`entry_details_id`),
  KEY `idx_qc_mes` (`mes_work_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='入库质检单表';

CREATE TABLE IF NOT EXISTS `wms_wave_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `wave_code` varchar(100) NOT NULL COMMENT '波次号',
  `warehouse_id` int NOT NULL COMMENT '仓库ID',
  `pick_mode` int NOT NULL DEFAULT 2 COMMENT '拣货模式(1摘果法 2播种法)',
  `logistics_company_id` bigint DEFAULT NULL COMMENT '承运商ID(分波维度之一)',
  `storage_condition` varchar(30) DEFAULT NULL COMMENT '存储温区(字典 wms_storage_condition，分波维度之一)',
  `order_count` int NOT NULL DEFAULT 0 COMMENT '订单数',
  `sku_count` int NOT NULL DEFAULT 0 COMMENT 'SKU种数',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总件数',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态(1待拣货 2拣货中 3已拣货 4分货中 5已完成 9已取消)',
  `emp_id` int DEFAULT NULL COMMENT '作业员工ID',
  `saas_id` varchar(60) DEFAULT NULL COMMENT 'SaaS标识',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_by` varchar(30) DEFAULT NULL COMMENT '创建人账号',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `update_by` varchar(30) DEFAULT NULL COMMENT '修改人账号',
  `is_del` int DEFAULT 1 COMMENT '是否删除 1未删除 2已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wave_code` (`wave_code`),
  KEY `idx_wave_status` (`status`),
  KEY `idx_wave_wh` (`warehouse_id`),
  KEY `idx_wave_group` (`logistics_company_id`,`storage_condition`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='出库波次表';
