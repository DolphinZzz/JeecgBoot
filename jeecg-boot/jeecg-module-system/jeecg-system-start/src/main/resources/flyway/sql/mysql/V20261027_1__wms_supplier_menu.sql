-- 供应商信息菜单。不建表，wms_supplier 已存在。
-- 一级目录 wms，二级目录 基础数据，其下为供应商信息页面。
-- 前台目录：views/wms

-- 一级菜单 wms（有子级，is_leaf=0，always_show=1 保证只有一个子菜单时仍显示本级）
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179307527735920', NULL, 'wms', '/wms', 'layouts/default/index', NULL, '/wms/base/wmsSupplierList', 0, NULL, '0', 14.00, 0, 'ant-design:inbox-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179307527735920');

-- 二级菜单 基础数据（有子级，is_leaf=0）
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179307527735921', '179307527735920', '基础数据', '/wms/base', 'layouts/default/index', NULL, '/wms/base/wmsSupplierList', 1, NULL, '0', 1.00, 0, 'ant-design:database-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179307527735921');

-- 供应商信息（有按钮子级，is_leaf=0）
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179307527735901', '179307527735921', '供应商信息', '/wms/base/wmsSupplierList', 'wms/WmsSupplier/WmsSupplierList', 'wms-wmsSupplier', NULL, 1, NULL, '0', 1.00, 0, 'ant-design:team-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179307527735901');

-- 新增
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179307527735902', '179307527735901', '添加供应商信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_supplier:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179307527735902');

-- 编辑
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179307527735903', '179307527735901', '编辑供应商信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_supplier:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179307527735903');

-- 删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179307527735904', '179307527735901', '删除供应商信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_supplier:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179307527735904');

-- 批量删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179307527735905', '179307527735901', '批量删除供应商信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_supplier:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179307527735905');

-- 导出excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179307527735906', '179307527735901', '导出excel_供应商信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_supplier:exportXls', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179307527735906');

-- 导入excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179307527735907', '179307527735901', '导入excel_供应商信息', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_supplier:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179307527735907');

-- 角色授权（admin角色）
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179307527735922', 'f6817f48af4fb3af11b9e8bf182f618b', '179307527735920', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179307527735922');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179307527735923', 'f6817f48af4fb3af11b9e8bf182f618b', '179307527735921', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179307527735923');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179307527735908', 'f6817f48af4fb3af11b9e8bf182f618b', '179307527735901', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179307527735908');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179307527735909', 'f6817f48af4fb3af11b9e8bf182f618b', '179307527735902', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179307527735909');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179307527735910', 'f6817f48af4fb3af11b9e8bf182f618b', '179307527735903', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179307527735910');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179307527735911', 'f6817f48af4fb3af11b9e8bf182f618b', '179307527735904', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179307527735911');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179307527735912', 'f6817f48af4fb3af11b9e8bf182f618b', '179307527735905', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179307527735912');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179307527735913', 'f6817f48af4fb3af11b9e8bf182f618b', '179307527735906', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179307527735913');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179307527735914', 'f6817f48af4fb3af11b9e8bf182f618b', '179307527735907', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179307527735914');

-- 省、市表字典白名单（下拉与列表翻译）
INSERT INTO sys_table_white_list(id, table_name, field_name, status, create_by, create_time)
SELECT '179307527735930', 'base_province', 'id,name', '1', 'admin', '2026-10-27 00:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_table_white_list WHERE table_name = 'base_province');

INSERT INTO sys_table_white_list(id, table_name, field_name, status, create_by, create_time)
SELECT '179307527735931', 'base_city', 'id,name,pid', '1', 'admin', '2026-10-27 00:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_table_white_list WHERE table_name = 'base_city');

-- 已插入过的环境：二级目录必须用 layouts/default/index，一级菜单跳到供应商页面
UPDATE sys_permission
SET component = 'layouts/default/index',
    redirect = '/wms/base/wmsSupplierList',
    perms_type = '0',
    always_show = 0
WHERE id = '179307527735920';

UPDATE sys_permission
SET component = 'layouts/default/index',
    redirect = '/wms/base/wmsSupplierList',
    perms_type = '0',
    always_show = 0
WHERE id = '179307527735921';

UPDATE sys_permission
SET component = 'wms/WmsSupplier/WmsSupplierList',
    component_name = 'wms-wmsSupplier',
    perms_type = '0',
    icon = 'ant-design:team-outlined'
WHERE id = '179307527735901';
