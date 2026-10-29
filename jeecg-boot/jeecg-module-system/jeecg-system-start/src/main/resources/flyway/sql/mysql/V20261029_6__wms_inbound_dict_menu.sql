-- WMS 三期（入库闭环）：字典与菜单
-- 菜单挂在一级目录「入库管理 /wms/inbound」，与「库存管理 /wms/stock」并列。
-- 全部 INSERT ... WHERE NOT EXISTS 固定 ID，重复执行不会重复插入。
--
-- ID 规则（ID 段必须与既有菜单/字典错开）：
--   字典：179330 + 两位字典序号  → 本脚本用 30/31
--   字典项：179340 + 两位字典序号 + 两位项序号
--   菜单：179341/179342/179343 + 9 位流水（179335~179338 已被二期占用）

-- ----------------------------------------------------------------
-- 1. 字典
-- ----------------------------------------------------------------

-- wms_entry_status 入库单状态（三期状态机，见方案 3.2.2）
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933030000000000000000000000000', '入库单状态', 'wms_entry_status', '入库单状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_entry_status');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934030010000000000000000000000', '17933030000000000000000000000000', '待到货', '10', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934030010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934030020000000000000000000000', '17933030000000000000000000000000', '收货清点中', '20', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934030020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934030030000000000000000000000', '17933030000000000000000000000000', '待质检', '30', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934030030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934030040000000000000000000000', '17933030000000000000000000000000', '质检完成待上架', '40', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934030040000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934030050000000000000000000000', '17933030000000000000000000000000', '上架中', '50', 5, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934030050000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934030060000000000000000000000', '17933030000000000000000000000000', '已完成入库', '60', 6, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934030060000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934030070000000000000000000000', '17933030000000000000000000000000', '异常挂起', '90', 7, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934030070000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934030080000000000000000000000', '17933030000000000000000000000000', '已取消', '99', 8, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934030080000000000000000000000');

-- wms_over_receive_status 超收放行状态（决策 D4）
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933031000000000000000000000000', '超收放行状态', 'wms_over_receive_status', '超收放行状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_over_receive_status');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934031010000000000000000000000', '17933031000000000000000000000000', '无需放行', '0', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934031010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934031020000000000000000000000', '17933031000000000000000000000000', '待主管确认', '1', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934031020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934031030000000000000000000000', '17933031000000000000000000000000', '已放行', '2', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934031030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934031040000000000000000000000', '17933031000000000000000000000000', '已驳回', '3', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934031040000000000000000000000');

-- ----------------------------------------------------------------
-- 2. 菜单
-- ----------------------------------------------------------------
-- ========== 2.0 入库管理（一级目录）==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179341000000000', '179307527735920', '入库管理', '/wms/inbound', 'layouts/default/index', NULL, NULL, 1, NULL, '0', 2.00, 0, 'ant-design:import-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179341000000000');

-- ========== 2.1 入库单 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179342000000001', '179341000000000', '入库单', '/wms/inbound/wmsEntryList', 'wms/WmsEntry/WmsEntryList', 'wms-wmsEntry', NULL, 1, NULL, '0', 1.00, 0, 'ant-design:file-add-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179342000000001');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179342000000002', '179342000000001', '新建入库单', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_warehousing_entry:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179342000000002');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179342000000003', '179342000000001', '收货清点', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_warehousing_entry:receive', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179342000000003');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179342000000004', '179342000000001', '超收放行', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_warehousing_entry:overReceiveAudit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179342000000004');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179342000000005', '179342000000001', '取消入库单', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_warehousing_entry:cancel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179342000000005');

-- ========== 2.2 质检单 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179343000000001', '179341000000000', '质检单', '/wms/inbound/wmsQualityCheckList', 'wms/WmsQualityCheck/WmsQualityCheckList', 'wms-wmsQualityCheck', NULL, 1, NULL, '0', 2.00, 0, 'ant-design:safety-certificate-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179343000000001');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179343000000002', '179343000000001', '提交质检', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_quality_check:submit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179343000000002');

-- ========== 2.3 上架作业 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179344000000001', '179341000000000', '上架作业', '/wms/inbound/wmsPutawayList', 'wms/WmsPutaway/WmsPutawayList', 'wms-wmsPutaway', NULL, 1, NULL, '0', 3.00, 0, 'ant-design:arrow-up-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179344000000001');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179344000000002', '179344000000001', '上架推荐', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_warehousing_entry_up:recommend', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179344000000002');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179344000000003', '179344000000001', '确认上架', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_warehousing_entry_up:confirm', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179344000000003');

-- ----------------------------------------------------------------
-- 3. 角色授权（管理员角色）
-- ----------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date, operate_ip)
SELECT REPLACE(p.id, '17934', '17944'), 'f6817f48af4fb3af11b9e8bf182f618b', p.id, NOW(), '127.0.0.1'
FROM sys_permission p
WHERE p.id IN (
    '179341000000000',
    '179342000000001','179342000000002','179342000000003','179342000000004','179342000000005',
    '179343000000001','179343000000002',
    '179344000000001','179344000000002','179344000000003'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.id = REPLACE(p.id, '17934', '17944')
);
