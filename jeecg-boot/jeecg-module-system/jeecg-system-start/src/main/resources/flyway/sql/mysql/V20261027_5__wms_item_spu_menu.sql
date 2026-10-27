-- 商品管理。不建表，wms_item_spu、wms_item_sku 已存在。主键改为数据库自增。
-- 挂在已有「基础数据」(179307527735921) 下。
-- 前台目录：views/wms

ALTER TABLE wms_item_spu
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID';
ALTER TABLE wms_item_sku
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID';

-- 商品管理（有按钮子级，is_leaf=0）
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179309069660801', '179307527735921', '商品管理', '/wms/base/wmsItemSpuList', 'wms/WmsItemSpu/WmsItemSpuList', 'wms-wmsItemSpu', NULL, 1, NULL, '0', 4.00, 0, 'ant-design:shopping-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179309069660801');

-- 新增
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179309069660802', '179309069660801', '添加商品管理', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_item_spu:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179309069660802');

-- 编辑
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179309069660803', '179309069660801', '编辑商品管理', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_item_spu:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179309069660803');

-- 删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179309069660804', '179309069660801', '删除商品管理', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_item_spu:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179309069660804');

-- 批量删除
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179309069660805', '179309069660801', '批量删除商品管理', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_item_spu:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179309069660805');

-- 导出excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179309069660806', '179309069660801', '导出excel_商品管理', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_item_spu:exportXls', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179309069660806');

-- 导入excel
INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179309069660807', '179309069660801', '导入excel_商品管理', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_item_spu:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-10-27 00:00:00', NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179309069660807');

-- 角色授权（admin角色）
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179309069660808', 'f6817f48af4fb3af11b9e8bf182f618b', '179309069660801', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179309069660808');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179309069660809', 'f6817f48af4fb3af11b9e8bf182f618b', '179309069660802', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179309069660809');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179309069660810', 'f6817f48af4fb3af11b9e8bf182f618b', '179309069660803', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179309069660810');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179309069660811', 'f6817f48af4fb3af11b9e8bf182f618b', '179309069660804', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179309069660811');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179309069660812', 'f6817f48af4fb3af11b9e8bf182f618b', '179309069660805', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179309069660812');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179309069660813', 'f6817f48af4fb3af11b9e8bf182f618b', '179309069660806', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179309069660813');
INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT '179309069660814', 'f6817f48af4fb3af11b9e8bf182f618b', '179309069660807', NULL, '2026-10-27 00:00:00', '127.0.0.1'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE id = '179309069660814');

-- 供应商、品牌下拉用的表字典白名单
INSERT INTO sys_table_white_list(id, table_name, field_name, status, create_by, create_time)
SELECT '179309069660815', 'wms_supplier', 'id,supplier_name', '1', 'admin', '2026-10-27 00:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_table_white_list WHERE table_name = 'wms_supplier');
INSERT INTO sys_table_white_list(id, table_name, field_name, status, create_by, create_time)
SELECT '179309069660816', 'wms_brand', 'id,name', '1', 'admin', '2026-10-27 00:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_table_white_list WHERE table_name = 'wms_brand');
