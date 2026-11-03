-- WMS 补齐库位商品规划与容量配置保存权限
-- 授权标识：wms:wms_inventory:capacity_edit
-- 挂载父级：179331000000001 (实时库存/库存管理)

SET NAMES utf8mb4;

-- 1. 插入按钮权限记录
INSERT INTO sys_permission (
  id, parent_id, name, url, component, is_route, component_name, redirect,
  menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
  keep_alive, hidden, hide_tab, description, create_by, create_time,
  update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT
  '179331000000003', '179331000000001', '配置商品规划', '/wms/wmsLocaItemCapacity/saveBatch', NULL, 0, NULL, NULL,
  2, 'wms:wms_inventory:capacity_edit', '1', 2.00, 0, NULL, 1,
  0, 0, 0, '库位配置允许存放的SPU及最大件数', 'admin', NOW(),
  NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id = '179331000000003' OR perms = 'wms:wms_inventory:capacity_edit');

-- 2. 授权给关键角色（管理员、仓管等）
INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date, operate_ip)
SELECT
    MD5(CONCAT(r.role_code, '_179331000000003')),
    r.id,
    '179331000000003',
    NOW(),
    '127.0.0.1'
FROM sys_role r
WHERE r.role_code IN ('admin', 'vue3', 'sh_wh_admin', 'gz_wh_admin', 'test')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = '179331000000003'
  );
