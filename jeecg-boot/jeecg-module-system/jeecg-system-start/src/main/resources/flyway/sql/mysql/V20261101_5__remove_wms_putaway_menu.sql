-- 下线上架作业独立菜单，并将上架操作权限挂载至入库单菜单下
-- 179344000000001: 上架作业菜单
-- 179342000000001: 入库单菜单

SET NAMES utf8mb4;

-- 1. 将上架推荐与确认上架按钮权限的 parent_id 调整为入库单菜单 (179342000000001)
UPDATE sys_permission
SET parent_id = '179342000000001'
WHERE id IN ('179344000000002', '179344000000003');

-- 2. 确保关键角色拥有入库单下的上架确认与推荐权限
INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date, operate_ip)
SELECT
    MD5(CONCAT(r.role_code, '_', p.id)),
    r.id,
    p.id,
    NOW(),
    '127.0.0.1'
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code IN ('admin', 'vue3', 'sh_wh_admin', 'gz_wh_admin', 'test')
  AND p.id IN ('179344000000002', '179344000000003')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 3. 清理角色与原上架作业菜单的映射关联
DELETE FROM sys_role_permission WHERE permission_id = '179344000000001';

-- 4. 删除原上架作业菜单记录
DELETE FROM sys_permission WHERE id = '179344000000001';
