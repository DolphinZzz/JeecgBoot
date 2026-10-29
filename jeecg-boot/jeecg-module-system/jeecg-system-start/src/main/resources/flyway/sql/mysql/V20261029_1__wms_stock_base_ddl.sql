-- WMS 二期：库存底座（移库移位 / 补货 / 盘点）所需的表结构调整。
-- 1. 库内作业单、盘点相关表主键改为数据库自增（与 V20261027_3、V20261028_11 的约定一致）；
-- 2. 作业单补充仓库维度与备注，便于按仓查询；操作员工允许为空（登录账号已由拦截器写入 create_by）；
-- 3. 盘点记录补充质量状态与批次号：只有 (库位, SKU, 批次, 货主, 质量状态) 五维才能定位到唯一库存行；
-- 4. 补充按业务查询的索引。

ALTER TABLE `wms_warehouse_operations`
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `emp_id` int NULL COMMENT '操作员工ID',
  MODIFY COLUMN `status` int NOT NULL DEFAULT 1 COMMENT '状态(1未确认 2已执行 3已取消)',
  ADD COLUMN `warehouse_id` int NULL COMMENT '仓库ID' AFTER `job_code`,
  ADD COLUMN `remark` varchar(255) NULL COMMENT '备注' AFTER `source_batch_id`,
  ADD COLUMN `create_by` varchar(30) NULL COMMENT '创建人账号' AFTER `create_time`,
  ADD COLUMN `update_by` varchar(30) NULL COMMENT '修改人账号' AFTER `update_time`,
  ADD KEY `idx_operation_wh_type` (`warehouse_id`, `operation_type`, `status`),
  ADD KEY `idx_operation_source_batch` (`source_batch_id`);

ALTER TABLE `wms_warehouse_operations_details`
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  ADD KEY `idx_op_detail_operation` (`warehouse_operations_id`),
  ADD KEY `idx_op_detail_sku` (`sku_id`),
  ADD KEY `idx_op_detail_target_loca` (`target_warehouse_area_loca_id`);

ALTER TABLE `wms_stocktaking_task`
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `emp_id` int NULL COMMENT '操作员工ID',
  ADD KEY `idx_stk_task_enable` (`is_enable`);

ALTER TABLE `wms_stocktaking_task_details`
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  ADD KEY `idx_stk_task_detail_task` (`stocktaking_task_id`);

ALTER TABLE `wms_stocktaking_info`
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `emp_id` int NULL COMMENT '操作员工ID',
  ADD COLUMN `quality_status` int NOT NULL DEFAULT 1 COMMENT '质量状态(1良品 2待检 3不良品)' AFTER `expire_date`,
  ADD COLUMN `batch_no` varchar(50) NULL COMMENT '盘点批次号(一次生成的一批盘点记录)' AFTER `stocktaking_task_id`,
  ADD KEY `idx_stk_info_batch` (`batch_no`),
  ADD KEY `idx_stk_info_task` (`stocktaking_task_id`),
  ADD KEY `idx_stk_info_loca` (`target_warehouse_area_loca_id`),
  ADD KEY `idx_stk_info_sku` (`sku_id`),
  ADD KEY `idx_stk_info_status` (`status`, `audit_status`);

ALTER TABLE `wms_stocktaking_loss`
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  MODIFY COLUMN `emp_id` int NULL COMMENT '操作员工ID',
  ADD KEY `idx_stk_loss_info` (`stocktaking_info`);
