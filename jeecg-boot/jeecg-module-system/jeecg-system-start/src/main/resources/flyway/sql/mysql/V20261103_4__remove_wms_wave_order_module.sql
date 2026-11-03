-- ====================================================================
-- 彻底删除 WMS 波次管理模块（转向纯轻量级单单拣货体系）
-- 日期: 2026-11-03
-- ====================================================================

-- 1. 出库单表剥离 wave_id 与 pick_mode
ALTER TABLE `wms_outbound_order` DROP INDEX `idx_outbound_wave`;
ALTER TABLE `wms_outbound_order` DROP COLUMN `wave_id`;
ALTER TABLE `wms_outbound_order` DROP COLUMN `pick_mode`;

-- 2. 物理删除波次主表
DROP TABLE IF EXISTS `wms_wave_order`;

-- 3. 清理系统角色权限关联
DELETE FROM `sys_role_permission` WHERE `permission_id` IN (
    '179352000000001',
    '179352000000002',
    '179352000000003',
    '179352000000004',
    '179352000000005'
);

-- 4. 清理系统菜单与按钮权限
DELETE FROM `sys_permission` WHERE `id` IN (
    '179352000000001',
    '179352000000002',
    '179352000000003',
    '179352000000004',
    '179352000000005'
);

-- 5. 清理波次相关数据字典与字典项
DELETE FROM `sys_dict_item` WHERE `dict_id` IN (
    '17933010000000000000000000000000', -- wms_pick_mode
    '17933041000000000000000000000000'  -- wms_wave_status
);

DELETE FROM `sys_dict` WHERE `id` IN (
    '17933010000000000000000000000000',
    '17933041000000000000000000000000'
);
