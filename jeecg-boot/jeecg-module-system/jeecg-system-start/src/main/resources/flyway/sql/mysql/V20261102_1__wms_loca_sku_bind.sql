-- ============================================================================
-- 库位与SKU绑定及无混批次策略：
-- 1. 存储区(STORAGE)与拣货区(PICKING)默认严格禁止混放SKU与混放批次
-- 2. 退货不良区(RETURN)保持允许混放
-- 3. 将当前在库且仅含一种SKU的库位，对齐写入 fixed_sku_id 建立绑定
-- 4. 规范化商品SPU存储条件数据，将中文常温/冷藏等对齐为标准枚举编码
-- 5. 为广州二号仓(warehouse_id=5)补齐存储区与拣货区标准货架库位
-- ============================================================================

-- 将属于存储区或拣货区的库位设置为禁止混放
UPDATE wms_warehouse_area_loca l
JOIN wms_warehouse_area a ON l.warehouse_area_id = a.id
SET l.allow_mix_sku = 0,
    l.allow_mix_batch = 0
WHERE a.category IN ('STORAGE', 'PICKING');

-- 退货不良区允许混放
UPDATE wms_warehouse_area_loca l
JOIN wms_warehouse_area a ON l.warehouse_area_id = a.id
SET l.allow_mix_sku = 1,
    l.allow_mix_batch = 1
WHERE a.category = 'RETURN';

-- 自动将当前在库单一SKU的存储区/拣货区库位回写 fixed_sku_id
UPDATE wms_warehouse_area_loca l
JOIN (
    SELECT warehouse_area_loca_id, MAX(sku_id) AS single_sku_id
    FROM wms_inventory
    WHERE is_del = 1 AND total_count > 0
    GROUP BY warehouse_area_loca_id
    HAVING COUNT(DISTINCT sku_id) = 1
) inv ON l.id = inv.warehouse_area_loca_id
JOIN wms_warehouse_area a ON l.warehouse_area_id = a.id
SET l.fixed_sku_id = inv.single_sku_id,
    l.loca_status = 2
WHERE a.category IN ('STORAGE', 'PICKING')
  AND (l.fixed_sku_id IS NULL OR l.fixed_sku_id = 0);

-- 规范化商品SPU存储温区数据
UPDATE wms_item_spu SET storage_condition = 'NORMAL' WHERE storage_condition = '常温';
UPDATE wms_item_spu SET storage_condition = 'COLD' WHERE storage_condition = '冷藏';
UPDATE wms_item_spu SET storage_condition = 'COOL' WHERE storage_condition = '阴凉';
UPDATE wms_item_spu SET storage_condition = 'FROZEN' WHERE storage_condition = '冷冻';
UPDATE wms_item_spu SET storage_condition = 'ESD' WHERE storage_condition = '防静电';
UPDATE wms_item_spu SET storage_condition = 'HAZARD' WHERE storage_condition = '危化';

-- 为广州二号仓 (warehouse_id = 5) 补齐基础货架库位（原料存储区A id=7、线边拣货区 id=9）
INSERT INTO `wms_warehouse_area_loca`
  (`warehouse_id`, `warehouse_area_id`, `loca_code`, `tunnel_number`, `shelf_number`, `layer_number`, `position_number`,
   `loca_len`, `loca_width`, `loca_height`, `local_volume`, `local_bearing_capacity`,
   `abc_class`, `storage_condition`, `loca_status`, `allow_mix_sku`, `allow_mix_batch`,
   `used_volume`, `used_weight`, `pick_distance`, `receive_distance`, `ergonomic_level`, `loca_score`, `is_del`, `create_time`, `create_by`)
SELECT 5, 7, 'GZ-01-A-01-01', '01', 'A', '01', '01', 1200, 1000, 500, 600000, 500000, 'A', 'NORMAL', 1, 0, 0, 0, 0, 10, 15, 1, 0.9500, 1, NOW(), 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `wms_warehouse_area_loca` WHERE `warehouse_id` = 5 AND `loca_code` = 'GZ-01-A-01-01');

INSERT INTO `wms_warehouse_area_loca`
  (`warehouse_id`, `warehouse_area_id`, `loca_code`, `tunnel_number`, `shelf_number`, `layer_number`, `position_number`,
   `loca_len`, `loca_width`, `loca_height`, `local_volume`, `local_bearing_capacity`,
   `abc_class`, `storage_condition`, `loca_status`, `allow_mix_sku`, `allow_mix_batch`,
   `used_volume`, `used_weight`, `pick_distance`, `receive_distance`, `ergonomic_level`, `loca_score`, `is_del`, `create_time`, `create_by`)
SELECT 5, 7, 'GZ-01-A-01-02', '01', 'A', '01', '02', 1200, 1000, 500, 600000, 500000, 'A', 'NORMAL', 1, 0, 0, 0, 0, 12, 16, 1, 0.9300, 1, NOW(), 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `wms_warehouse_area_loca` WHERE `warehouse_id` = 5 AND `loca_code` = 'GZ-01-A-01-02');

INSERT INTO `wms_warehouse_area_loca`
  (`warehouse_id`, `warehouse_area_id`, `loca_code`, `tunnel_number`, `shelf_number`, `layer_number`, `position_number`,
   `loca_len`, `loca_width`, `loca_height`, `local_volume`, `local_bearing_capacity`,
   `abc_class`, `storage_condition`, `loca_status`, `allow_mix_sku`, `allow_mix_batch`,
   `used_volume`, `used_weight`, `pick_distance`, `receive_distance`, `ergonomic_level`, `loca_score`, `is_del`, `create_time`, `create_by`)
SELECT 5, 7, 'GZ-01-A-02-01', '01', 'A', '02', '01', 1200, 1000, 500, 600000, 500000, 'B', 'NORMAL', 1, 0, 0, 0, 0, 15, 18, 2, 0.8500, 1, NOW(), 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `wms_warehouse_area_loca` WHERE `warehouse_id` = 5 AND `loca_code` = 'GZ-01-A-02-01');

INSERT INTO `wms_warehouse_area_loca`
  (`warehouse_id`, `warehouse_area_id`, `loca_code`, `tunnel_number`, `shelf_number`, `layer_number`, `position_number`,
   `loca_len`, `loca_width`, `loca_height`, `local_volume`, `local_bearing_capacity`,
   `abc_class`, `storage_condition`, `loca_status`, `allow_mix_sku`, `allow_mix_batch`,
   `used_volume`, `used_weight`, `pick_distance`, `receive_distance`, `ergonomic_level`, `loca_score`, `is_del`, `create_time`, `create_by`)
SELECT 5, 7, 'GZ-01-A-02-02', '01', 'A', '02', '02', 1200, 1000, 500, 600000, 500000, 'B', 'NORMAL', 1, 0, 0, 0, 0, 17, 20, 2, 0.8200, 1, NOW(), 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `wms_warehouse_area_loca` WHERE `warehouse_id` = 5 AND `loca_code` = 'GZ-01-A-02-02');

INSERT INTO `wms_warehouse_area_loca`
  (`warehouse_id`, `warehouse_area_id`, `loca_code`, `tunnel_number`, `shelf_number`, `layer_number`, `position_number`,
   `loca_len`, `loca_width`, `loca_height`, `local_volume`, `local_bearing_capacity`,
   `abc_class`, `storage_condition`, `loca_status`, `allow_mix_sku`, `allow_mix_batch`,
   `used_volume`, `used_weight`, `pick_distance`, `receive_distance`, `ergonomic_level`, `loca_score`, `is_del`, `create_time`, `create_by`)
SELECT 5, 9, 'GZ-02-A-01-01', '02', 'A', '01', '01', 1000,  800, 400, 320000, 300000, 'A', 'NORMAL', 1, 0, 0, 0, 0,  8, 12, 1, 0.9600, 1, NOW(), 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `wms_warehouse_area_loca` WHERE `warehouse_id` = 5 AND `loca_code` = 'GZ-02-A-01-01');

INSERT INTO `wms_warehouse_area_loca`
  (`warehouse_id`, `warehouse_area_id`, `loca_code`, `tunnel_number`, `shelf_number`, `layer_number`, `position_number`,
   `loca_len`, `loca_width`, `loca_height`, `local_volume`, `local_bearing_capacity`,
   `abc_class`, `storage_condition`, `loca_status`, `allow_mix_sku`, `allow_mix_batch`,
   `used_volume`, `used_weight`, `pick_distance`, `receive_distance`, `ergonomic_level`, `loca_score`, `is_del`, `create_time`, `create_by`)
SELECT 5, 9, 'GZ-02-A-01-02', '02', 'A', '01', '02', 1000,  800, 400, 320000, 300000, 'A', 'NORMAL', 1, 0, 0, 0, 0, 10, 14, 1, 0.9400, 1, NOW(), 'admin'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `wms_warehouse_area_loca` WHERE `warehouse_id` = 5 AND `loca_code` = 'GZ-02-A-01-02');
