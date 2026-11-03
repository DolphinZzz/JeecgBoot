-- 注意：该页面对应的前台目录为views/mes/workshop文件夹下
-- 如果你想更改到其他目录，请修改sql中component字段对应的值


-- 主菜单
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
VALUES ('179371437435501', NULL, 'mes_workshop', '/mes/workshop/mesWorkshopList', 'mes/workshop/MesWorkshopList', NULL, NULL, 0, NULL, '1', 0.00, 0, NULL, 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', '2026-11-03 21:59:34', NULL, NULL, 0);

-- 新增
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179371437435502', '179371437435501', '添加mes_workshop', NULL, NULL, 0, NULL, NULL, 2, 'mes:mes_workshop:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-11-03 21:59:34', NULL, NULL, 0, 0, '1', 0);

-- 编辑
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179371437435503', '179371437435501', '编辑mes_workshop', NULL, NULL, 0, NULL, NULL, 2, 'mes:mes_workshop:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-11-03 21:59:34', NULL, NULL, 0, 0, '1', 0);

-- 删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179371437435504', '179371437435501', '删除mes_workshop', NULL, NULL, 0, NULL, NULL, 2, 'mes:mes_workshop:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-11-03 21:59:34', NULL, NULL, 0, 0, '1', 0);

-- 批量删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179371437435505', '179371437435501', '批量删除mes_workshop', NULL, NULL, 0, NULL, NULL, 2, 'mes:mes_workshop:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-11-03 21:59:34', NULL, NULL, 0, 0, '1', 0);

-- 导出excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179371437435506', '179371437435501', '导出excel_mes_workshop', NULL, NULL, 0, NULL, NULL, 2, 'mes:mes_workshop:exportXls', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-11-03 21:59:34', NULL, NULL, 0, 0, '1', 0);

-- 导入excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES ('179371437435507', '179371437435501', '导入excel_mes_workshop', NULL, NULL, 0, NULL, NULL, 2, 'mes:mes_workshop:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-11-03 21:59:34', NULL, NULL, 0, 0, '1', 0);

-- 角色授权（以 admin 角色为例，role_id 可替换）
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179371437435508', 'f6817f48af4fb3af11b9e8bf182f618b', '179371437435501', NULL, '2026-11-03 21:59:34', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179371437435509', 'f6817f48af4fb3af11b9e8bf182f618b', '179371437435502', NULL, '2026-11-03 21:59:34', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179371437435510', 'f6817f48af4fb3af11b9e8bf182f618b', '179371437435503', NULL, '2026-11-03 21:59:34', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179371437435511', 'f6817f48af4fb3af11b9e8bf182f618b', '179371437435504', NULL, '2026-11-03 21:59:34', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179371437435512', 'f6817f48af4fb3af11b9e8bf182f618b', '179371437435505', NULL, '2026-11-03 21:59:34', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179371437435513', 'f6817f48af4fb3af11b9e8bf182f618b', '179371437435506', NULL, '2026-11-03 21:59:34', '127.0.0.1');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip) VALUES ('179371437435514', 'f6817f48af4fb3af11b9e8bf182f618b', '179371437435507', NULL, '2026-11-03 21:59:34', '127.0.0.1');