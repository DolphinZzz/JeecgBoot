-- WMS 质检不合格退货闭环处理
-- 质检送检有不合格品时，分流放置到退货货架区域（RETURN库区），联系对应供应商退货处置

-- 必须显式声明连接字符集，否则通过 mysql 客户端执行时中文会被按 latin1 二次编码成乱码
SET NAMES utf8mb4;

SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_quality_check` ADD COLUMN `return_loca_id` int NULL COMMENT ''退货放置库位ID'' AFTER `return_order_no`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_quality_check' AND column_name='return_loca_id');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_quality_check` ADD COLUMN `return_status` int NOT NULL DEFAULT 0 COMMENT ''退货状态(0无需退货 1待退货处理 2已放置退货区待退供应商 3已退还供应商)'' AFTER `return_loca_id`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_quality_check' AND column_name='return_status');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_quality_check` ADD COLUMN `return_remark` varchar(500) NULL COMMENT ''退货联系处置备注'' AFTER `return_status`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_quality_check' AND column_name='return_remark');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_quality_check` ADD COLUMN `return_handle_time` datetime NULL COMMENT ''退货处理时间'' AFTER `return_remark`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_quality_check' AND column_name='return_handle_time');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_quality_check` ADD COLUMN `return_handle_by` varchar(50) NULL COMMENT ''退货处理人'' AFTER `return_handle_time`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_quality_check' AND column_name='return_handle_by');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 索引
SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_quality_check` ADD KEY `idx_qc_return` (`return_status`, `return_loca_id`)',
  'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='wms_quality_check' AND index_name='idx_qc_return');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 字典：wms_qc_return_status
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933044000000000000000000000000', '质检退货状态', 'wms_qc_return_status', '质检不合格品退货处置状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_qc_return_status');

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934044000000000000000000000000', '17933044000000000000000000000000', '无需退货', '0', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE dict_id = '17933044000000000000000000000000' AND item_value = '0');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934044001000000000000000000000', '17933044000000000000000000000000', '待退货处理', '1', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE dict_id = '17933044000000000000000000000000' AND item_value = '1');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934044002000000000000000000000', '17933044000000000000000000000000', '已入退货区待退供应商', '2', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE dict_id = '17933044000000000000000000000000' AND item_value = '2');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934044003000000000000000000000', '17933044000000000000000000000000', '已退还供应商', '3', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE dict_id = '17933044000000000000000000000000' AND item_value = '3');

-- 为每个已有仓库补充一个默认退货不良库区与货架库位（若无）
INSERT INTO `wms_warehouse_area` (`warehouse_id`, `name`, `category`, `zone_type`, `storage_condition`, `create_time`, `create_by`, `is_del`)
SELECT w.id, '退货不良品区', 'RETURN', 'GENERAL', 'NORMAL', NOW(), 'admin', 1
FROM `wms_warehouse` w
WHERE w.is_del = 1
  AND NOT EXISTS (
    SELECT 1 FROM `wms_warehouse_area` a WHERE a.warehouse_id = w.id AND a.category = 'RETURN' AND a.is_del = 1
  );

-- 为退货库区补充一个退货货架库位（若无）
INSERT INTO `wms_warehouse_area_loca` (
  `warehouse_id`, `warehouse_area_id`, `loca_code`,
  `loca_len`, `loca_width`, `loca_height`, `local_volume`, `local_bearing_capacity`,
  `tunnel_number`, `shelf_number`, `layer_number`, `position_number`,
  `abc_class`, `storage_condition`, `loca_status`, `allow_mix_sku`, `allow_mix_batch`,
  `used_volume`, `used_weight`, `ergonomic_level`,
  `create_time`, `create_by`, `is_del`
)
SELECT
  a.warehouse_id, a.id, 'R-01-01',
  2000, 2000, 2000, 8000000, 5000000,
  '1', '1', '1', '1',
  'C', 'NORMAL', 1, 1, 1,
  0, 0, 2,
  NOW(), 'admin', 1
FROM `wms_warehouse_area` a
WHERE a.category = 'RETURN' AND a.is_del = 1
  AND NOT EXISTS (
    SELECT 1 FROM `wms_warehouse_area_loca` l WHERE l.warehouse_area_id = a.id AND l.is_del = 1
  );

-- 按钮权限：退货处理、确认退回供应商
INSERT INTO sys_permission (id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179343000000003', '179343000000001', '退货处理', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_quality_check:return', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179343000000003');

INSERT INTO sys_permission (id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179343000000004', '179343000000001', '确认退还供应商', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_quality_check:shipReturn', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179343000000004');

-- 授权给管理员角色 admin
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT MD5(CONCAT('admin_179343000000003')), r.id, '179343000000003'
FROM sys_role r WHERE r.role_code = 'admin'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = r.id AND permission_id = '179343000000003');

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT MD5(CONCAT('admin_179343000000004')), r.id, '179343000000004'
FROM sys_role r WHERE r.role_code = 'admin'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = r.id AND permission_id = '179343000000004');
