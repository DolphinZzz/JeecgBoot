-- 商品类别菜单。不建表，wms_item_category 已存在。
-- 挂在已有「基础数据」(179307527735921) 下。
-- 前台目录：views/wms

-- 商品类别设置（有按钮子级，is_leaf=0）
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179308423398501', '179307527735921', '商品类别设置', '/wms/base/wmsItemCategoryList', 'wms/WmsItemCategory/WmsItemCategoryList', 'wms-wmsItemCategory', NULL, 1, NULL, '0', 2.00, 0, 'ant-design:apartment-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308423398501');

-- 新增
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179308423398502', '179308423398501', '添加商品类别设置', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_item_category:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308423398502');

-- 编辑
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179308423398503', '179308423398501', '编辑商品类别设置', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_item_category:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308423398503');

-- 删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179308423398504', '179308423398501', '删除商品类别设置', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_item_category:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308423398504');

-- 批量删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179308423398505', '179308423398501', '批量删除商品类别设置', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_item_category:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308423398505');

-- 导出excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179308423398506', '179308423398501', '导出excel_商品类别设置', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_item_category:exportXls', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308423398506');

-- 导入excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179308423398507', '179308423398501', '导入excel_商品类别设置', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_item_category:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179308423398507');

-- 角色授权（admin角色）
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308423398508', 'f6817f48af4fb3af11b9e8bf182f618b', '179308423398501', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308423398508');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308423398509', 'f6817f48af4fb3af11b9e8bf182f618b', '179308423398502', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308423398509');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308423398510', 'f6817f48af4fb3af11b9e8bf182f618b', '179308423398503', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308423398510');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308423398511', 'f6817f48af4fb3af11b9e8bf182f618b', '179308423398504', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308423398511');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308423398512', 'f6817f48af4fb3af11b9e8bf182f618b', '179308423398505', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308423398512');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308423398513', 'f6817f48af4fb3af11b9e8bf182f618b', '179308423398506', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308423398513');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179308423398514', 'f6817f48af4fb3af11b9e8bf182f618b', '179308423398507', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179308423398514');

-- 商品类别表字典白名单（所属类别树选择、列表翻译）
INSERT INTO sys_table_white_list(id, table_name, field_name, status, create_by, create_time)
SELECT '179308423398515', 'wms_item_category', 'id,name,parent_id', '1', 'admin', '2026-10-27 00:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_table_white_list WHERE table_name = 'wms_item_category');
