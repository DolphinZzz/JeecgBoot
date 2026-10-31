-- WMS 四期（出库闭环）：表结构优化、字典与菜单
--
-- 1. 表结构优化：
--    - wms_outbound_order 补齐 warehouse_id, consignor_id，主键改为自增，字段可空/默认值修正
--    - wms_outbound_order_details 补齐 consignor_id，主键改为自增，增加索引
--    - wms_outbound_order_package 补齐 outbound_order_id，主键改为自增
--    - wms_outbound_order_weights 补齐 outbound_order_id，主键改为自增
--    - wms_logistics_company 主键改为自增，并初始化常用物流承运商
-- 2. 字典：
--    - wms_outbound_status 出库单状态 (10/20/30/40/50/60/70/80/90/91/92)
--    - wms_wave_status 波次状态 (1/2/3/4/5/9)
--    - wms_review_status 复核状态 (0/1/2)
--    - wms_shortage_handle 缺货处理 (1/2)
-- 3. 菜单：
--    - 一级目录：出库管理 /wms/outbound
--    - 二级菜单：出库单、波次管理、拣货任务、复核打包、物流发货及配套按钮权限

-- ----------------------------------------------------------------
-- 1. 表结构升级
-- ----------------------------------------------------------------

-- 1.1 wms_outbound_order 主键改为自增
SET @sql := (SELECT IF(
  (SELECT EXTRA FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wms_outbound_order' AND column_name='id') LIKE '%auto_increment%',
  'SELECT 1',
  'ALTER TABLE `wms_outbound_order` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'''
));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.2 wms_outbound_order 补充 warehouse_id
SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_outbound_order` ADD COLUMN `warehouse_id` int NULL COMMENT ''仓库ID'' AFTER `customer_id`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_outbound_order' AND column_name='warehouse_id');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.3 wms_outbound_order 补充 consignor_id
SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_outbound_order` ADD COLUMN `consignor_id` bigint NULL COMMENT ''货主ID'' AFTER `warehouse_id`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_outbound_order' AND column_name='consignor_id');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.4 wms_outbound_order 字段约束调整：status 默认值设为 10，emp_id 和 price 允许为空
ALTER TABLE `wms_outbound_order`
  MODIFY COLUMN `status` int NOT NULL DEFAULT 10 COMMENT '出库状态(10待分配 20已分配 30待补货 40拣货中 50待复核 60待打包 70待发货 80已发货 90已签收 91已取消 92异常挂起)',
  MODIFY COLUMN `emp_id` int NULL COMMENT '经办员工ID',
  MODIFY COLUMN `price` int NULL DEFAULT 0 COMMENT '订单金额',
  MODIFY COLUMN `total_weight` int NOT NULL DEFAULT 0 COMMENT '总重量',
  MODIFY COLUMN `total_volume` int NOT NULL DEFAULT 0 COMMENT '总体积';

-- 1.5 wms_outbound_order 索引补齐
SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_outbound_order` ADD KEY `idx_outbound_wh_status` (`warehouse_id`, `status`)',
  'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='wms_outbound_order' AND index_name='idx_outbound_wh_status');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.6 wms_outbound_order_details 主键改为自增
SET @sql := (SELECT IF(
  (SELECT EXTRA FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wms_outbound_order_details' AND column_name='id') LIKE '%auto_increment%',
  'SELECT 1',
  'ALTER TABLE `wms_outbound_order_details` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'''
));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.7 wms_outbound_order_details 补充 consignor_id
SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_outbound_order_details` ADD COLUMN `consignor_id` bigint NULL COMMENT ''货主ID'' AFTER `sku_id`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_outbound_order_details' AND column_name='consignor_id');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.8 wms_outbound_order_details 补充索引
SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_outbound_order_details` ADD KEY `idx_details_order` (`outbound_order_id`)',
  'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='wms_outbound_order_details' AND index_name='idx_details_order');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.9 wms_outbound_order_package 主键自增并补齐 outbound_order_id
SET @sql := (SELECT IF(
  (SELECT EXTRA FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wms_outbound_order_package' AND column_name='id') LIKE '%auto_increment%',
  'SELECT 1',
  'ALTER TABLE `wms_outbound_order_package` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'''
));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_outbound_order_package` ADD COLUMN `outbound_order_id` bigint NULL COMMENT ''出库单ID'' AFTER `package_code`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_outbound_order_package' AND column_name='outbound_order_id');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_outbound_order_package` ADD KEY `idx_package_order` (`outbound_order_id`)',
  'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='wms_outbound_order_package' AND index_name='idx_package_order');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.10 wms_outbound_order_weights 主键自增并补齐 outbound_order_id
SET @sql := (SELECT IF(
  (SELECT EXTRA FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wms_outbound_order_weights' AND column_name='id') LIKE '%auto_increment%',
  'SELECT 1',
  'ALTER TABLE `wms_outbound_order_weights` MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT ''主键ID'''
));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_outbound_order_weights` ADD COLUMN `outbound_order_id` bigint NULL COMMENT ''出库单ID'' AFTER `weights_code`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_outbound_order_weights' AND column_name='outbound_order_id');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_outbound_order_weights` ADD KEY `idx_weights_order` (`outbound_order_id`)',
  'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='wms_outbound_order_weights' AND index_name='idx_weights_order');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.11 wms_logistics_company 主键自增与常用承运商初始化
SET @sql := (SELECT IF(
  (SELECT EXTRA FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wms_logistics_company' AND column_name='id') LIKE '%auto_increment%',
  'SELECT 1',
  'ALTER TABLE `wms_logistics_company` MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT COMMENT ''主键ID'''
));
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

INSERT INTO `wms_logistics_company` (`company_name`, `min_price`, `link_user`, `linke_phone`, `sort`, `is_del`, `create_time`, `create_by`)
SELECT '顺丰速运', 1200, '顺丰客服', '95338', 1, 1, NOW(), 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `wms_logistics_company` WHERE `company_name` = '顺丰速运');

INSERT INTO `wms_logistics_company` (`company_name`, `min_price`, `link_user`, `linke_phone`, `sort`, `is_del`, `create_time`, `create_by`)
SELECT '德邦快递', 1000, '德邦客服', '95353', 2, 1, NOW(), 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `wms_logistics_company` WHERE `company_name` = '德邦快递');

INSERT INTO `wms_logistics_company` (`company_name`, `min_price`, `link_user`, `linke_phone`, `sort`, `is_del`, `create_time`, `create_by`)
SELECT '京东物流', 1100, '京东客服', '950616', 3, 1, NOW(), 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `wms_logistics_company` WHERE `company_name` = '京东物流');

INSERT INTO `wms_logistics_company` (`company_name`, `min_price`, `link_user`, `linke_phone`, `sort`, `is_del`, `create_time`, `create_by`)
SELECT '中通快递', 800, '中通客服', '95311', 4, 1, NOW(), 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `wms_logistics_company` WHERE `company_name` = '中通快递');

-- ----------------------------------------------------------------
-- 2. 字典
-- ----------------------------------------------------------------

-- 2.1 wms_outbound_status 出库单状态 (方案 3.3.2)
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933040000000000000000000000000', '出库单状态', 'wms_outbound_status', '出库单状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_outbound_status');

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934040010000000000000000000000', '17933040000000000000000000000000', '待分配', '10', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934040010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934040020000000000000000000000', '17933040000000000000000000000000', '已分配/已锁库', '20', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934040020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934040030000000000000000000000', '17933040000000000000000000000000', '待补货', '30', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934040030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934040040000000000000000000000', '17933040000000000000000000000000', '拣货中', '40', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934040040000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934040050000000000000000000000', '17933040000000000000000000000000', '拣货完成待复核', '50', 5, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934040050000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934040060000000000000000000000', '17933040000000000000000000000000', '复核通过待打包', '60', 6, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934040060000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934040070000000000000000000000', '17933040000000000000000000000000', '已打包待发货', '70', 7, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934040070000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934040080000000000000000000000', '17933040000000000000000000000000', '已发货', '80', 8, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934040080000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934040090000000000000000000000', '17933040000000000000000000000000', '已签收', '90', 9, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934040090000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934040100000000000000000000000', '17933040000000000000000000000000', '已取消', '91', 10, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934040100000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934040110000000000000000000000', '17933040000000000000000000000000', '异常挂起', '92', 11, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934040110000000000000000000000');

-- 2.2 wms_wave_status 波次状态 (方案 4.2.5)
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933041000000000000000000000000', '波次状态', 'wms_wave_status', '波次状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_wave_status');

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934041010000000000000000000000', '17933041000000000000000000000000', '待拣货', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934041010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934041020000000000000000000000', '17933041000000000000000000000000', '拣货中', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934041020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934041030000000000000000000000', '17933041000000000000000000000000', '已拣货', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934041030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934041040000000000000000000000', '17933041000000000000000000000000', '分货中', '4', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934041040000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934041050000000000000000000000', '17933041000000000000000000000000', '已完成', '5', 5, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934041050000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934041090000000000000000000000', '17933041000000000000000000000000', '已取消', '9', 6, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934041090000000000000000000000');

-- 2.3 wms_review_status 复核状态
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933042000000000000000000000000', '复核状态', 'wms_review_status', '出库明细复核状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_review_status');

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934042000000000000000000000000', '17933042000000000000000000000000', '未复核', '0', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934042000000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934042010000000000000000000000', '17933042000000000000000000000000', '复核通过', '1', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934042010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934042020000000000000000000000', '17933042000000000000000000000000', '复核不通过', '2', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934042020000000000000000000000');

-- 2.4 wms_shortage_handle 缺货处理方式
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933043000000000000000000000000', '缺货处理', 'wms_shortage_handle', '出库缺货处理策略', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_shortage_handle');

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934043010000000000000000000000', '17933043000000000000000000000000', '剔除缺货行', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934043010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934043020000000000000000000000', '17933043000000000000000000000000', '整单挂起待补货', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934043020000000000000000000000');

-- ----------------------------------------------------------------
-- 3. 菜单与权限
-- ----------------------------------------------------------------

-- ========== 3.0 出库管理（一级目录 /wms/outbound）==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179350000000000', '179307527735920', '出库管理', '/wms/outbound', 'layouts/default/index', NULL, NULL, 1, NULL, '0', 3.00, 0, 'ant-design:export-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179350000000000');

-- ========== 3.1 出库单管理 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179351000000001', '179350000000000', '出库单', '/wms/outbound/wmsOutboundList', 'wms/WmsOutbound/WmsOutboundList', 'wms-wmsOutbound', NULL, 1, NULL, '0', 1.00, 0, 'ant-design:file-done-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179351000000001');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179351000000002', '179351000000001', '新建出库单', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_outbound_order:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179351000000002');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179351000000003', '179351000000001', '库存预检分配', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_outbound_order:allocate', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179351000000003');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179351000000004', '179351000000001', '取消出库单', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_outbound_order:cancel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179351000000004');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179351000000005', '179351000000001', '删除出库单', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_outbound_order:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179351000000005');

-- ========== 3.2 波次管理 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179352000000001', '179350000000000', '波次管理', '/wms/outbound/wmsWaveList', 'wms/WmsWave/WmsWaveList', 'wms-wmsWave', NULL, 1, NULL, '0', 2.00, 0, 'ant-design:branches-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179352000000001');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179352000000002', '179352000000001', '自动分波', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_wave_order:generate', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179352000000002');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179352000000003', '179352000000001', '开始拣货', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_wave_order:startPick', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179352000000003');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179352000000004', '179352000000001', '完成分货', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_wave_order:completeDistribute', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179352000000004');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179352000000005', '179352000000001', '取消波次', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_wave_order:cancel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179352000000005');

-- ========== 3.3 拣货任务 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179353000000001', '179350000000000', '拣货任务', '/wms/outbound/wmsPickList', 'wms/WmsOutbound/WmsPickList', 'wms-wmsPick', NULL, 1, NULL, '0', 3.00, 0, 'ant-design:shopping-cart-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179353000000001');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179353000000002', '179353000000001', '确认实拣', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_outbound_order:pickConfirm', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179353000000002');

-- ========== 3.4 复核打包 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179354000000001', '179350000000000', '复核打包', '/wms/outbound/wmsReviewPackageList', 'wms/WmsOutbound/WmsReviewPackageList', 'wms-wmsReviewPackage', NULL, 1, NULL, '0', 4.00, 0, 'ant-design:inbox-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179354000000001');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179354000000002', '179354000000001', '复核确认', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_outbound_order:reviewConfirm', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179354000000002');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179354000000003', '179354000000001', '打包称重', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_outbound_order:packageWeight', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179354000000003');

-- ========== 3.5 物流发货 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179355000000001', '179350000000000', '物流发货', '/wms/outbound/wmsShipList', 'wms/WmsOutbound/WmsShipList', 'wms-wmsShip', NULL, 1, NULL, '0', 5.00, 0, 'ant-design:send-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179355000000001');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179355000000002', '179355000000001', '发货确认', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_outbound_order:shipConfirm', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179355000000002');

-- ========== 3.6 承运商管理（基础数据） ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179356000000001', '179307527735921', '承运商管理', '/wms/base/wmsLogisticsList', 'wms/WmsLogistics/WmsLogisticsList', 'wms-wmsLogistics', NULL, 1, NULL, '0', 7.00, 0, 'ant-design:car-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179356000000001');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179356000000002', '179356000000001', '添加承运商', NULL, NULL, 0, NULL, NULL, 2, 'wms:logistics:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179356000000002');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179356000000003', '179356000000001', '编辑承运商', NULL, NULL, 0, NULL, NULL, 2, 'wms:logistics:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179356000000003');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179356000000004', '179356000000001', '删除承运商', NULL, NULL, 0, NULL, NULL, 2, 'wms:logistics:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179356000000004');

-- ----------------------------------------------------------------
-- 4. 表字典白名单
-- ----------------------------------------------------------------
INSERT INTO sys_table_white_list(id, table_name, field_name, status, create_by, create_time)
SELECT '179350000000010', 'wms_logistics_company', 'id,company_name,is_del', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_table_white_list WHERE table_name = 'wms_logistics_company');

-- ----------------------------------------------------------------
-- 5. 角色授权（管理员与相关角色）
-- ----------------------------------------------------------------
-- 管理员角色 f6817f48af4fb3af11b9e8bf182f618b
INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date, operate_ip)
SELECT REPLACE(CONCAT('17945', SUBSTRING(p.id, 6)), '-', ''), 'f6817f48af4fb3af11b9e8bf182f618b', p.id, NOW(), '127.0.0.1'
FROM sys_permission p
WHERE p.id IN (
    '179350000000000',
    '179351000000001','179351000000002','179351000000003','179351000000004','179351000000005',
    '179352000000001','179352000000002','179352000000003','179352000000004','179352000000005',
    '179353000000001','179353000000002',
    '179354000000001','179354000000002','179354000000003',
    '179355000000001','179355000000002',
    '179356000000001','179356000000002','179356000000003','179356000000004'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = 'f6817f48af4fb3af11b9e8bf182f618b' AND rp.permission_id = p.id
);

-- Vue3角色 1501570619841810433
INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date, operate_ip)
SELECT REPLACE(CONCAT('17946', SUBSTRING(p.id, 6)), '-', ''), '1501570619841810433', p.id, NOW(), '127.0.0.1'
FROM sys_permission p
WHERE p.id IN (
    '179350000000000',
    '179351000000001','179351000000002','179351000000003','179351000000004','179351000000005',
    '179352000000001','179352000000002','179352000000003','179352000000004','179352000000005',
    '179353000000001','179353000000002',
    '179354000000001','179354000000002','179354000000003',
    '179355000000001','179355000000002',
    '179356000000001','179356000000002','179356000000003','179356000000004'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = '1501570619841810433' AND rp.permission_id = p.id
);

-- 仓库管理员角色 (sh_wh_admin, gz_wh_admin)
INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date, operate_ip)
SELECT REPLACE(CONCAT('17947', SUBSTRING(p.id, 6)), '-', ''), '2115683251967868930', p.id, NOW(), '127.0.0.1'
FROM sys_permission p
WHERE p.id IN (
    '179350000000000',
    '179351000000001','179351000000002','179351000000003','179351000000004','179351000000005',
    '179352000000001','179352000000002','179352000000003','179352000000004','179352000000005',
    '179353000000001','179353000000002',
    '179354000000001','179354000000002','179354000000003',
    '179355000000001','179355000000002',
    '179356000000001','179356000000002','179356000000003','179356000000004'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = '2115683251967868930' AND rp.permission_id = p.id
);

INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date, operate_ip)
SELECT REPLACE(CONCAT('17948', SUBSTRING(p.id, 6)), '-', ''), '2115685628582150146', p.id, NOW(), '127.0.0.1'
FROM sys_permission p
WHERE p.id IN (
    '179350000000000',
    '179351000000001','179351000000002','179351000000003','179351000000004','179351000000005',
    '179352000000001','179352000000002','179352000000003','179352000000004','179352000000005',
    '179353000000001','179353000000002',
    '179354000000001','179354000000002','179354000000003',
    '179355000000001','179355000000002',
    '179356000000001','179356000000002','179356000000003','179356000000004'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = '2115685628582150146' AND rp.permission_id = p.id
);

