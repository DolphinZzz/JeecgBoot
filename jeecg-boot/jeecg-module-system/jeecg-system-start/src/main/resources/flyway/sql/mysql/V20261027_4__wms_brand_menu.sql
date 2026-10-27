-- 品牌管理。不建表，wms_brand 已存在。主键改为数据库自增。
-- 挂在已有「基础数据」(179307527735921) 下。
-- 前台目录：views/wms

ALTER TABLE wms_brand
    MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID';

-- 品牌管理（有按钮子级，is_leaf=0）
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179308952183201', '179307527735921', '品牌管理', '/wms/base/wmsBrandList', 'wms/WmsBrand/WmsBrandList', 'wms-wmsBrand', NULL, 1, NULL, '0', 3.00, 0, 'ant-design:tag-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308952183201');

-- 新增
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179308952183202', '179308952183201', '添加品牌管理', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_brand:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308952183202');

-- 编辑
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179308952183203', '179308952183201', '编辑品牌管理', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_brand:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308952183203');

-- 删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179308952183204', '179308952183201', '删除品牌管理', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_brand:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308952183204');

-- 批量删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179308952183205', '179308952183201', '批量删除品牌管理', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_brand:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308952183205');

-- 导出excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179308952183206', '179308952183201', '导出excel_品牌管理', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_brand:exportXls', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308952183206');

-- 导入excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179308952183207', '179308952183201', '导入excel_品牌管理', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_brand:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308952183207');

-- 角色授权（admin角色）
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308952183208', 'f6817f48af4fb3af11b9e8bf182f618b', '179308952183201', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308952183208');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308952183209', 'f6817f48af4fb3af11b9e8bf182f618b', '179308952183202', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308952183209');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308952183210', 'f6817f48af4fb3af11b9e8bf182f618b', '179308952183203', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308952183210');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308952183211', 'f6817f48af4fb3af11b9e8bf182f618b', '179308952183204', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308952183211');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308952183212', 'f6817f48af4fb3af11b9e8bf182f618b', '179308952183205', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308952183212');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308952183213', 'f6817f48af4fb3af11b9e8bf182f618b', '179308952183206', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308952183213');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308952183214', 'f6817f48af4fb3af11b9e8bf182f618b', '179308952183207', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308952183214');
