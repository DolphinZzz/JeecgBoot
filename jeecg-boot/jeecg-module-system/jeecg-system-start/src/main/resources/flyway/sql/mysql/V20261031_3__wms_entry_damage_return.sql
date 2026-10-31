-- WMS 收货清点破损数量落库 + 破损自动入退货不良区
-- 破损件在收货时即计入明细，并自动放置到退货不良区（RETURN）货架，等待联系供应商退换
-- 注意：必须显式声明连接字符集，否则通过 mysql 客户端执行时中文会被按 latin1 二次编码成乱码
SET NAMES utf8mb4;

-- 1. 明细表增加收货破损数量
SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_warehousing_entry_details` ADD COLUMN `damage_count` int NOT NULL DEFAULT 0 COMMENT ''收货破损数量'' AFTER `received_count`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_warehousing_entry_details' AND column_name='damage_count');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. 质检单增加来源标识：破损件在收货时自动生成一条「破损退货」记录，复用退货处置与退还供应商流程
SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_quality_check` ADD COLUMN `source` int NOT NULL DEFAULT 1 COMMENT ''来源(1质检 2收货破损)'' AFTER `qc_type`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_quality_check' AND column_name='source');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 3. 字典：质检单来源
INSERT INTO sys_dict (id, dict_name, dict_code, description, del_flag, create_by, create_time, type, tenant_id)
SELECT '17933045000000000000000000000000', '质检单来源', 'wms_qc_source', '质检单来源（质检/收货破损）', 0, 'admin', NOW(), 1, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_code = 'wms_qc_source');

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934045000000000000000000000000', '17933045000000000000000000000000', '质检', '1', 1, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE dict_id = '17933045000000000000000000000000' AND item_value = '1');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, sort_order, status, create_by, create_time)
SELECT '17934045001000000000000000000000', '17933045000000000000000000000000', '收货破损', '2', 2, 1, 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_item WHERE dict_id = '17933045000000000000000000000000' AND item_value = '2');
