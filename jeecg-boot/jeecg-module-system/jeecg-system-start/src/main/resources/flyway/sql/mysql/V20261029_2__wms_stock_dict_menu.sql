-- WMS 二期：库存管理的字典与菜单（实时库存 / 库存流水 / 移库移位 / 补货作业 / 盘点管理 / SN追溯）
-- 菜单挂在 wms 下的一级目录「库存管理 /wms/stock」，与 V20261028_12 的仓储管理同级。
-- 全部 INSERT ... WHERE NOT EXISTS 固定 ID，重复执行不会重复插入。
-- 字典 ID 采用与 V20261028_9 相同的定宽序列：179330 + 两位字典序号，179340 + 两位字典序号 + 两位项序号，右补零至 32 位。

-- ----------------------------------------------------------------
-- 1. 补充字典（二期新增的状态与盘点/损溢取值，既有 V20261028_9 未覆盖）
-- ----------------------------------------------------------------

-- wms_operation_status 库内作业状态
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933023000000000000000000000000', '库内作业状态', 'wms_operation_status', '库内作业状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_operation_status');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934023010000000000000000000000', '17933023000000000000000000000000', '未确认', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934023010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934023020000000000000000000000', '17933023000000000000000000000000', '已执行', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934023020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934023030000000000000000000000', '17933023000000000000000000000000', '已取消', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934023030000000000000000000000');

-- wms_expire_status 效期状态
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933024000000000000000000000000', '库存效期状态', 'wms_expire_status', '库存效期状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_expire_status');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934024010000000000000000000000', '17933024000000000000000000000000', '正常', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934024010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934024020000000000000000000000', '17933024000000000000000000000000', '临期', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934024020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934024030000000000000000000000', '17933024000000000000000000000000', '过期', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934024030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934024040000000000000000000000', '17933024000000000000000000000000', '无有效期', '4', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934024040000000000000000000000');

-- wms_stocktaking_category 盘点类型
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933025000000000000000000000000', '盘点类型', 'wms_stocktaking_category', '盘点类型', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_stocktaking_category');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934025010000000000000000000000', '17933025000000000000000000000000', '滚动盘点', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934025010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934025020000000000000000000000', '17933025000000000000000000000000', '定时盘点', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934025020000000000000000000000');

-- wms_stocktaking_status 盘点状态
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933026000000000000000000000000', '盘点状态', 'wms_stocktaking_status', '盘点状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_stocktaking_status');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934026010000000000000000000000', '17933026000000000000000000000000', '待盘点', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934026010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934026020000000000000000000000', '17933026000000000000000000000000', '已盘待审核', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934026020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934026030000000000000000000000', '17933026000000000000000000000000', '已确认', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934026030000000000000000000000');

-- wms_stocktaking_audit_status 盘点差异审批状态
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933027000000000000000000000000', '盘点差异审批状态', 'wms_stocktaking_audit_status', '盘点差异审批状态', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_stocktaking_audit_status');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934027010000000000000000000000', '17933027000000000000000000000000', '无差异', '0', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934027010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934027020000000000000000000000', '17933027000000000000000000000000', '待审批', '1', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934027020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934027030000000000000000000000', '17933027000000000000000000000000', '已通过', '2', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934027030000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934027040000000000000000000000', '17933027000000000000000000000000', '已驳回', '3', 4, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934027040000000000000000000000');

-- wms_loss_type 损溢类型
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933028000000000000000000000000', '盘点损溢类型', 'wms_loss_type', '盘点损溢类型', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_loss_type');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934028010000000000000000000000', '17933028000000000000000000000000', '盘亏', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934028010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934028020000000000000000000000', '17933028000000000000000000000000', '盘盈', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934028020000000000000000000000');

-- wms_loss_category 损溢分类
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933029000000000000000000000000', '盘点损溢分类', 'wms_loss_category', '盘点损溢分类', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_loss_category');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934029010000000000000000000000', '17933029000000000000000000000000', '差异', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934029010000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934029020000000000000000000000', '17933029000000000000000000000000', '破损', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934029020000000000000000000000');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934029030000000000000000000000', '17933029000000000000000000000000', '丢失', '3', 3, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE id = '17934029030000000000000000000000');

-- ----------------------------------------------------------------
-- 2. 库存管理 目录
-- ----------------------------------------------------------------
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179330000000000', '179307527735920', '库存管理', '/wms/stock', 'layouts/default/index', NULL, NULL, 1, NULL, '0', 3.00, 0, 'ant-design:database-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179330000000000');

-- ========== 2.1 实时库存 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179331000000001', '179330000000000', '实时库存', '/wms/stock/wmsInventoryList', 'wms/WmsInventory/WmsInventoryList', 'wms-wmsInventory', NULL, 1, NULL, '0', 1.00, 0, 'ant-design:inbox-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179331000000001');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179331000000002', '179331000000001', '库存总量核对', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_inventory:verify', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179331000000002');

-- ========== 2.2 库存流水 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179332000000001', '179330000000000', '库存流水', '/wms/stock/wmsInventoryLogList', 'wms/WmsInventoryLog/WmsInventoryLogList', 'wms-wmsInventoryLog', NULL, 1, NULL, '0', 2.00, 0, 'ant-design:history-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179332000000001');

-- ========== 2.3 移库移位 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179333000000001', '179330000000000', '移库移位', '/wms/stock/wmsMoveList', 'wms/WmsMove/WmsMoveList', 'wms-wmsMove', NULL, 1, NULL, '0', 3.00, 0, 'ant-design:swap-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179333000000001');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179333000000002', '179333000000001', '新建移库单', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_warehouse_operations:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179333000000002');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179333000000003', '179333000000001', '执行作业单', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_warehouse_operations:execute', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179333000000003');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179333000000004', '179333000000001', '取消作业单', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_warehouse_operations:cancel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179333000000004');

-- ========== 2.4 补货作业 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179334000000001', '179330000000000', '补货作业', '/wms/stock/wmsReplenishList', 'wms/WmsReplenish/WmsReplenishList', 'wms-wmsReplenish', NULL, 1, NULL, '0', 4.00, 0, 'ant-design:retweet-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179334000000001');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179334000000002', '179334000000001', '生成补货单', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_warehouse_operations:generateReplenish', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179334000000002');

-- ========== 2.5 盘点管理 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179335000000001', '179330000000000', '盘点管理', '/wms/stock/wmsStocktakingList', 'wms/WmsStocktaking/WmsStocktakingList', 'wms-wmsStocktaking', NULL, 1, NULL, '0', 5.00, 0, 'ant-design:audit-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179335000000001');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179335000000002', '179335000000001', '生成盘点任务', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_stocktaking:create', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179335000000002');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179335000000003', '179335000000001', '录入实盘数', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_stocktaking:count', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179335000000003');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179335000000004', '179335000000001', '差异审批', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_stocktaking:audit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179335000000004');

INSERT INTO sys_permission(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
SELECT '179335000000005', '179335000000001', '结案调账', NULL, NULL, 0, NULL, NULL, 2, 'wms:wms_stocktaking:confirm', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179335000000005');

-- ========== 2.6 SN追溯 ==========
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time, update_by, update_time, internal_or_external)
SELECT '179336000000001', '179330000000000', 'SN追溯', '/wms/stock/wmsItemSnList', 'wms/WmsItemSn/WmsItemSnList', 'wms-wmsItemSn', NULL, 1, NULL, '0', 6.00, 0, 'ant-design:barcode-outlined', 1, 0, 0, 0, 0, NULL, '1', 0, 0, 'admin', NOW(), NULL, NULL, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179336000000001');

-- ----------------------------------------------------------------
-- 3. 角色授权（admin 角色 f6817f48af4fb3af11b9e8bf182f618b）
-- ----------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date, operate_ip)
SELECT REPLACE(p.id, '17933', '17939'), 'f6817f48af4fb3af11b9e8bf182f618b', p.id, NOW(), '127.0.0.1'
FROM sys_permission p
WHERE p.id LIKE '17933%'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.id = REPLACE(p.id, '17933', '17939')
  );
