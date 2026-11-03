-- ============================================================================
-- 彻底移除库区分区类型与商品分区推导规则：
-- 1. 移除 wms_warehouse_area 表的 zone_type 字段
-- 2. 清理 wms_zone_type 字典定义及字典项
-- 3. 清理 wms_strategy_config 中商品分区规则参数 (config_group = 'ZONE')
-- ============================================================================

-- 1. 移除 wms_warehouse_area 表的分区类型字段
ALTER TABLE `wms_warehouse_area` DROP COLUMN `zone_type`;

-- 2. 清理 wms_zone_type 字典定义及字典项
DELETE FROM `sys_dict_item` WHERE `dict_id` IN (SELECT `id` FROM `sys_dict` WHERE `dict_code` = 'wms_zone_type');
DELETE FROM `sys_dict` WHERE `dict_code` = 'wms_zone_type';

-- 3. 清理商品分区规则策略配置
DELETE FROM `wms_strategy_config` WHERE `config_group` = 'ZONE';
