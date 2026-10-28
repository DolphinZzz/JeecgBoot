-- 客户信息表与菜单权限
-- 菜单位置：wms-基础数据 (parent_id: 179307527735921)
-- 前台目录：views/wms/WmsCustomer

-- ========== 1. 建表 DDL ==========
CREATE TABLE IF NOT EXISTS `wms_customer` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `customer_name` varchar(100) NOT NULL COMMENT '客户名称',
  `link_name` varchar(50) DEFAULT NULL COMMENT '负责人',
  `link_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `province_id` int DEFAULT NULL COMMENT '所在省份',
  `city_code` int DEFAULT NULL COMMENT '所在城市',
  `address` varchar(255) DEFAULT NULL COMMENT '详情地址',
  `saas_id` varchar(32) DEFAULT NULL COMMENT 'SaaS标识',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新日期',
  `is_del` tinyint NOT NULL DEFAULT '1' COMMENT '是否删除 1未删除 2已删除',
  PRIMARY KEY (`id`),
  KEY `idx_customer_name` (`customer_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户信息';

-- ========== 2. 菜单权限 ==========
-- 客户信息菜单（有按钮子级，is_leaf=0）
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179316478566601', '179307527735921', '客户信息', '/wms/base/wmsCustomerList', 'wms/WmsCustomer/WmsCustomerList', 'wms-wmsCustomer', NULL, 1, NULL, '0', 5.00, 0, 'ant-design:usergroup-add-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', '2026-10-28 00:00:00', NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179316478566601');

-- 新增
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179316478566602', '179316478566601', '添加客户信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_customer:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-28 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179316478566602');

-- 编辑
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179316478566603', '179316478566601', '编辑客户信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_customer:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-28 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179316478566603');

-- 删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179316478566604', '179316478566601', '删除客户信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_customer:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-28 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179316478566604');

-- 批量删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179316478566605', '179316478566601', '批量删除客户信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_customer:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-28 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179316478566605');

-- 导出excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179316478566606', '179316478566601', '导出excel_客户信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_customer:exportXls', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-28 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179316478566606');

-- 导入excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179316478566607', '179316478566601', '导入excel_客户信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_customer:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-28 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179316478566607');

-- ========== 3. 角色授权（admin角色） ==========
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179316478566608', 'f6817f48af4fb3af11b9e8bf182f618b', '179316478566601', NULL, '2026-10-28 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179316478566608');

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179316478566609', 'f6817f48af4fb3af11b9e8bf182f618b', '179316478566602', NULL, '2026-10-28 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179316478566609');

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179316478566610', 'f6817f48af4fb3af11b9e8bf182f618b', '179316478566603', NULL, '2026-10-28 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179316478566610');

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179316478566611', 'f6817f48af4fb3af11b9e8bf182f618b', '179316478566604', NULL, '2026-10-28 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179316478566611');

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179316478566612', 'f6817f48af4fb3af11b9e8bf182f618b', '179316478566605', NULL, '2026-10-28 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179316478566612');

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179316478566613', 'f6817f48af4fb3af11b9e8bf182f618b', '179316478566606', NULL, '2026-10-28 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179316478566613');

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179316478566614', 'f6817f48af4fb3af11b9e8bf182f618b', '179316478566607', NULL, '2026-10-28 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179316478566614');
