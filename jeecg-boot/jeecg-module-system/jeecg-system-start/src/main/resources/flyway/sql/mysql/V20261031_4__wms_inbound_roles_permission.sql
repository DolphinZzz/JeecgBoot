-- WMS 入库闭环与质检退货权限补齐授权
-- 覆盖角色：admin, vue3, sh_wh_admin, gz_wh_admin, sh_wh_staff, gz_wh_staff, test

SET NAMES utf8mb4;

-- 授权入库管理目录、入库单、质检单、上架作业及其全部按钮权限给关键业务角色
INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date, operate_ip)
SELECT
    MD5(CONCAT(r.role_code, '_', p.id)),
    r.id,
    p.id,
    NOW(),
    '127.0.0.1'
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code IN ('admin', 'vue3', 'sh_wh_admin', 'gz_wh_admin', 'sh_wh_staff', 'gz_wh_staff', 'test')
  AND p.id IN (
    '179341000000000', -- 入库管理目录
    '179342000000001', -- 入库单菜单
    '179342000000002', -- 新建入库单 (wms:wms_warehousing_entry:add)
    '179342000000003', -- 收货清点 (wms:wms_warehousing_entry:receive)
    '179342000000004', -- 超收放行 (wms:wms_warehousing_entry:overReceiveAudit)
    '179342000000005', -- 取消入库单 (wms:wms_warehousing_entry:cancel)
    '179343000000001', -- 质检单菜单
    '179343000000002', -- 录入质检结论 (wms:wms_quality_check:submit)
    '179343000000003', -- 退货处置 (wms:wms_quality_check:return)
    '179343000000004', -- 确认退还供应商 (wms:wms_quality_check:shipReturn)
    '179344000000001', -- 上架作业菜单
    '179344000000002', -- 上架推荐 (wms:wms_warehousing_entry_up:recommend)
    '179344000000003'  -- 确认上架 (wms:wms_warehousing_entry_up:confirm)
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
