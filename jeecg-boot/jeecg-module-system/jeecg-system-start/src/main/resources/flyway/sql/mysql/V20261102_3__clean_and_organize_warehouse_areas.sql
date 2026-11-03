-- ============================================================================
-- 库区库位数据规范化与空壳无用库区清理：
-- 1. 清理历史上无库位、无业务引用的 0 库位测试空壳库区 (ID: 3, 4, 5, 6, 8, 10, 11)
-- 2. 为广州二号仓原料存储区A、线边拣货区补充规范空库位，提升移库目标库区容量
-- ============================================================================

-- 1. 清理无库位、无库存引用的空壳冗余库区
DELETE FROM `wms_warehouse_area` WHERE `id` IN (3, 4, 5, 6, 8, 10, 11);

-- 2. 为广州二号仓【原料存储区A】(warehouse_area_id=7, warehouse_id=5) 扩充规范空库位
INSERT INTO `wms_warehouse_area_loca`
(`warehouse_id`, `warehouse_area_id`, `loca_code`, `tunnel_number`, `shelf_number`, `layer_number`, `position_number`,
 `loca_status`, `loca_len`, `loca_width`, `loca_height`, `local_volume`, `local_bearing_capacity`,
 `abc_class`, `is_del`, `storage_condition`, `fixed_sku_id`, `allow_mix_sku`, `allow_mix_batch`,
 `create_time`, `create_by`)
VALUES
(5, 7, 'GZ-01-B-01-01', '01', 'B', '1', '1', 1, 1200, 1000, 1500, 1800000, 1000000, 'A', 1, 'NORMAL', NULL, 0, 0, NOW(), 'admin'),
(5, 7, 'GZ-01-B-01-02', '01', 'B', '1', '2', 1, 1200, 1000, 1500, 1800000, 1000000, 'A', 1, 'NORMAL', NULL, 0, 0, NOW(), 'admin'),
(5, 7, 'GZ-01-B-02-01', '01', 'B', '2', '1', 1, 1200, 1000, 1500, 1800000, 1000000, 'B', 1, 'NORMAL', NULL, 0, 0, NOW(), 'admin'),
(5, 7, 'GZ-01-B-02-02', '01', 'B', '2', '2', 1, 1200, 1000, 1500, 1800000, 1000000, 'B', 1, 'NORMAL', NULL, 0, 0, NOW(), 'admin'),
(5, 7, 'GZ-01-B-03-01', '01', 'B', '3', '1', 1, 1200, 1000, 1500, 1800000, 1000000, 'B', 1, 'NORMAL', NULL, 0, 0, NOW(), 'admin'),
(5, 7, 'GZ-01-B-03-02', '01', 'B', '3', '2', 1, 1200, 1000, 1500, 1800000, 1000000, 'B', 1, 'NORMAL', NULL, 0, 0, NOW(), 'admin'),
(5, 7, 'GZ-01-B-04-01', '01', 'B', '4', '1', 1, 1200, 1000, 1500, 1800000, 1000000, 'C', 1, 'NORMAL', NULL, 0, 0, NOW(), 'admin'),
(5, 7, 'GZ-01-B-04-02', '01', 'B', '4', '2', 1, 1200, 1000, 1500, 1800000, 1000000, 'C', 1, 'NORMAL', NULL, 0, 0, NOW(), 'admin');

-- 3. 为广州二号仓【线边拣货区】(warehouse_area_id=9, warehouse_id=5) 扩充规范空库位
INSERT INTO `wms_warehouse_area_loca`
(`warehouse_id`, `warehouse_area_id`, `loca_code`, `tunnel_number`, `shelf_number`, `layer_number`, `position_number`,
 `loca_status`, `loca_len`, `loca_width`, `loca_height`, `local_volume`, `local_bearing_capacity`,
 `abc_class`, `is_del`, `storage_condition`, `fixed_sku_id`, `allow_mix_sku`, `allow_mix_batch`,
 `create_time`, `create_by`)
VALUES
(5, 9, 'GZ-02-A-02-01', '02', 'A', '2', '1', 1, 1200, 1000, 1200, 1440000, 800000, 'A', 1, 'NORMAL', NULL, 0, 0, NOW(), 'admin'),
(5, 9, 'GZ-02-A-02-02', '02', 'A', '2', '2', 1, 1200, 1000, 1200, 1440000, 800000, 'A', 1, 'NORMAL', NULL, 0, 0, NOW(), 'admin');
