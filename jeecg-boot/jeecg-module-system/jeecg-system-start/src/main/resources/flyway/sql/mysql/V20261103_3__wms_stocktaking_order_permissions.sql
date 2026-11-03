-- ====================================================================
-- WMS 盘库单按钮权限定义与管理员角色赋权
-- 日期: 2026-11-03
-- ====================================================================

-- 1. 注册按钮权限
INSERT INTO sys_permission (id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179335000000006', '179335000000001', '暂存草稿', NULL, NULL, 0, NULL, NULL, 2, 'wms:stocktaking_order:add', '1', 1, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179335000000006');

INSERT INTO sys_permission (id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179335000000007', '179335000000001', '完成盘点', NULL, NULL, 0, NULL, NULL, 2, 'wms:stocktaking_order:complete', '1', 2, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179335000000007');

INSERT INTO sys_permission (id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179335000000008', '179335000000001', '删除草稿', NULL, NULL, 0, NULL, NULL, 2, 'wms:stocktaking_order:delete', '1', 3, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179335000000008');

-- 2. 授权给管理员角色 (admin, role_id='f6817f48af4fb3af11b9e8bf182f618b')
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179335000000006', 'f6817f48af4fb3af11b9e8bf182f618b', '179335000000006', NULL, NOW(), '127.0.0.1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 'f6817f48af4fb3af11b9e8bf182f618b' AND permission_id = '179335000000006');

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179335000000007', 'f6817f48af4fb3af11b9e8bf182f618b', '179335000000007', NULL, NOW(), '127.0.0.1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 'f6817f48af4fb3af11b9e8bf182f618b' AND permission_id = '179335000000007');

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179335000000008', 'f6817f48af4fb3af11b9e8bf182f618b', '179335000000008', NULL, NOW(), '127.0.0.1'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 'f6817f48af4fb3af11b9e8bf182f618b' AND permission_id = '179335000000008');
