-- ============================================================
-- WMS 库位标准化重建与 SPU 规划容量初始化脚本
-- 适用范围：清空旧库位及历史脏库存，建立 4 段式标准货架，预先绑定 SPU 与梯次在库数据
-- ============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1. 彻底清空旧库存、流水、盘点、作业及库位数据
-- ------------------------------------------------------------
DELETE FROM wms_inventory_log;
DELETE FROM wms_inventory;
DELETE FROM wms_item_sn;
DELETE FROM wms_stocktaking_loss;
DELETE FROM wms_stocktaking_info;
DELETE FROM wms_warehouse_operations_details;
DELETE FROM wms_warehouse_operations;
DELETE FROM wms_loca_item_capacity;
DELETE FROM wms_warehouse_area_loca;

ALTER TABLE wms_inventory_log AUTO_INCREMENT = 1;
ALTER TABLE wms_inventory AUTO_INCREMENT = 1;
ALTER TABLE wms_item_sn AUTO_INCREMENT = 1;
ALTER TABLE wms_loca_item_capacity AUTO_INCREMENT = 1;
ALTER TABLE wms_warehouse_area_loca AUTO_INCREMENT = 1;

-- 补齐快速查询索引（如已存在忽略错误）
-- ALTER TABLE `wms_loca_item_capacity` ADD KEY `idx_wh_spu_del` (`warehouse_id`, `spu_id`, `is_del`);

-- ------------------------------------------------------------
-- 2. 建立 4 段式标准化货架库位 (以 仓库1: 上海一号仓 为主)
--    库区 1: 常温存储区A (STORAGE, GENERAL, NORMAL)
--    库区 2: 拣货区B     (PICKING, SMALL, NORMAL)
--    库区 12: 退货不良品区(RETURN, GENERAL, NORMAL)
-- ------------------------------------------------------------

-- 2.1 常温存储区A (库区1)：巷道 01，货架 A 和 B，各 4 层，每层 2 位
INSERT INTO `wms_warehouse_area_loca`
  (`id`, `warehouse_id`, `warehouse_area_id`, `loca_code`, `tunnel_number`, `shelf_number`, `layer_number`, `position_number`,
   `loca_len`, `loca_width`, `loca_height`, `local_volume`, `local_bearing_capacity`,
   `abc_class`, `storage_condition`, `loca_status`, `allow_mix_sku`, `allow_mix_batch`,
   `used_volume`, `used_weight`, `pick_distance`, `receive_distance`, `ergonomic_level`, `loca_score`, `is_del`, `create_time`, `create_by`)
VALUES
-- 货架 01-A (1~2层黄金层，3层次优层，4层不便层)
(1,  1, 1, '01-A-01-01', '01', 'A', '01', '01', 1200, 1000, 500, 600000, 500000, 'A', 'NORMAL', 2, 0, 1, 74900, 34000, 10, 15, 1, 0.9500, 1, NOW(), 'admin'),
(2,  1, 1, '01-A-01-02', '01', 'A', '01', '02', 1200, 1000, 500, 600000, 500000, 'A', 'NORMAL', 2, 0, 1, 33705, 15300, 12, 16, 1, 0.9300, 1, NOW(), 'admin'),
(3,  1, 1, '01-A-02-01', '01', 'A', '02', '01', 1200, 1000, 500, 600000, 500000, 'A', 'NORMAL', 1, 0, 1,     0,     0, 14, 17, 1, 0.9100, 1, NOW(), 'admin'),
(4,  1, 1, '01-A-02-02', '01', 'A', '02', '02', 1200, 1000, 500, 600000, 500000, 'A', 'NORMAL', 2, 0, 1, 136920, 72000, 16, 18, 1, 0.8900, 1, NOW(), 'admin'),
(5,  1, 1, '01-A-03-01', '01', 'A', '03', '01', 1200, 1000, 500, 600000, 500000, 'B', 'NORMAL', 2, 0, 1, 57050, 30000, 18, 20, 2, 0.7800, 1, NOW(), 'admin'),
(6,  1, 1, '01-A-03-02', '01', 'A', '03', '02', 1200, 1000, 500, 600000, 500000, 'B', 'NORMAL', 2, 0, 1, 277200,116000, 20, 22, 2, 0.7600, 1, NOW(), 'admin'),
(7,  1, 1, '01-A-04-01', '01', 'A', '04', '01', 1200, 1000, 500, 600000, 500000, 'C', 'NORMAL', 2, 0, 1, 103950, 43500, 22, 25, 3, 0.6000, 1, NOW(), 'admin'),
(8,  1, 1, '01-A-04-02', '01', 'A', '04', '02', 1200, 1000, 500, 600000, 500000, 'C', 'NORMAL', 1, 0, 1,     0,     0, 24, 27, 3, 0.5800, 1, NOW(), 'admin'),

-- 货架 01-B (涵盖混合库位)
(9,  1, 1, '01-B-01-01', '01', 'B', '01', '01', 1200, 1000, 500, 600000, 500000, 'A', 'NORMAL', 2, 0, 1, 11520, 10800, 11, 16, 1, 0.9400, 1, NOW(), 'admin'),
(10, 1, 1, '01-B-01-02', '01', 'B', '01', '02', 1200, 1000, 500, 600000, 500000, 'A', 'NORMAL', 1, 0, 1,     0,     0, 13, 17, 1, 0.9200, 1, NOW(), 'admin'),
(11, 1, 1, '01-B-02-01', '01', 'B', '02', '01', 1200, 1000, 500, 600000, 500000, 'A', 'NORMAL', 2, 0, 1, 169650, 41000, 15, 19, 1, 0.9000, 1, NOW(), 'admin'),
(12, 1, 1, '01-B-02-02', '01', 'B', '02', '02', 1200, 1000, 500, 600000, 500000, 'A', 'NORMAL', 2, 0, 1, 33930,  8200, 17, 21, 1, 0.8800, 1, NOW(), 'admin'),
(13, 1, 1, '01-B-03-01', '01', 'B', '03', '01', 1200, 1000, 500, 600000, 500000, 'B', 'NORMAL', 2, 1, 1, 19780, 11300, 19, 23, 2, 0.7700, 1, NOW(), 'admin'),
(14, 1, 1, '01-B-03-02', '01', 'B', '03', '02', 1200, 1000, 500, 600000, 500000, 'B', 'NORMAL', 1, 0, 1,     0,     0, 21, 25, 2, 0.7500, 1, NOW(), 'admin');

-- 2.2 拣货区B (库区2)：巷道 02，货架 A 和 B，各 2 层，每层 2 位
INSERT INTO `wms_warehouse_area_loca`
  (`id`, `warehouse_id`, `warehouse_area_id`, `loca_code`, `tunnel_number`, `shelf_number`, `layer_number`, `position_number`,
   `loca_len`, `loca_width`, `loca_height`, `local_volume`, `local_bearing_capacity`,
   `abc_class`, `storage_condition`, `loca_status`, `allow_mix_sku`, `allow_mix_batch`,
   `used_volume`, `used_weight`, `pick_distance`, `receive_distance`, `ergonomic_level`, `loca_score`, `is_del`, `create_time`, `create_by`)
VALUES
(15, 1, 2, '02-A-01-01', '02', 'A', '01', '01', 1000, 800, 400, 320000, 200000, 'A', 'NORMAL', 2, 0, 1, 14980,  6800, 5, 25, 1, 0.9800, 1, NOW(), 'admin'),
(16, 1, 2, '02-A-01-02', '02', 'A', '01', '02', 1000, 800, 400, 320000, 200000, 'A', 'NORMAL', 1, 0, 1,     0,     0, 6, 26, 1, 0.9700, 1, NOW(), 'admin'),
(17, 1, 2, '02-A-02-01', '02', 'A', '02', '01', 1000, 800, 400, 320000, 200000, 'B', 'NORMAL', 1, 0, 1,     0,     0, 7, 28, 1, 0.8800, 1, NOW(), 'admin'),
(18, 1, 2, '02-B-01-01', '02', 'B', '01', '01', 1000, 800, 400, 320000, 200000, 'A', 'NORMAL', 1, 0, 1,     0,     0, 6, 26, 1, 0.9600, 1, NOW(), 'admin'),
(19, 1, 2, '02-B-01-02', '02', 'B', '01', '02', 1000, 800, 400, 320000, 200000, 'A', 'NORMAL', 1, 0, 1,     0,     0, 8, 29, 1, 0.9500, 1, NOW(), 'admin');

-- 2.3 退货不良品区 (库区12)：巷道 03，货架 R
INSERT INTO `wms_warehouse_area_loca`
  (`id`, `warehouse_id`, `warehouse_area_id`, `loca_code`, `tunnel_number`, `shelf_number`, `layer_number`, `position_number`,
   `loca_len`, `loca_width`, `loca_height`, `local_volume`, `local_bearing_capacity`,
   `abc_class`, `storage_condition`, `loca_status`, `allow_mix_sku`, `allow_mix_batch`,
   `used_volume`, `used_weight`, `pick_distance`, `receive_distance`, `ergonomic_level`, `loca_score`, `is_del`, `create_time`, `create_by`)
VALUES
(20, 1, 12, '03-R-01-01', '03', 'R', '01', '01', 1200, 1000, 500, 600000, 500000, 'C', 'NORMAL', 1, 1, 1, 0, 0, 35, 10, 1, 0.5000, 1, NOW(), 'admin'),
(21, 1, 12, '03-R-01-02', '03', 'R', '01', '02', 1200, 1000, 500, 600000, 500000, 'C', 'NORMAL', 1, 1, 1, 0, 0, 36, 12, 1, 0.4800, 1, NOW(), 'admin');

-- ------------------------------------------------------------
-- 3. 库位预先绑定 SPU 规划与容量设置 (wms_loca_item_capacity)
-- ------------------------------------------------------------
-- 商品对照：
-- SPU 2: 数字万用表 (ELEC-DMM-001)
-- SPU 3: 手持示波器 (ELEC-OSC-001)
-- SPU 4: 锂电充电钻 (ELEC-DRL-001)
-- SPU 5: 激光测距仪 (ELEC-LDM-001)
-- SPU 6: 热敏标签打印机 (ELEC-LBL-001)

INSERT INTO `wms_loca_item_capacity`
  (`warehouse_id`, `warehouse_area_id`, `warehouse_area_loca_id`, `spu_id`, `max_count`, `remark`, `create_by`, `create_time`, `is_del`)
VALUES
-- === SPU 2 (数字万用表) 规划库位 ===
(1, 1, 1,  2, 100, '数字万用表-主存储位(满)', 'admin', NOW(), 1),
(1, 1, 2,  2, 100, '数字万用表-主存储位(半)', 'admin', NOW(), 1),
(1, 1, 3,  2, 100, '数字万用表-主存储位(空)', 'admin', NOW(), 1),
(1, 1, 13, 2,  50, '混合库位-万用表分位',     'admin', NOW(), 1),
(1, 2, 15, 2,  50, '数字万用表-拣货位',       'admin', NOW(), 1),
(1, 12, 20, 2, 50, '数字万用表-退货不良位',   'admin', NOW(), 1),

-- === SPU 3 (手持示波器) 规划库位 ===
(1, 1, 4,  3,  60, '手持示波器-存储位(满)',   'admin', NOW(), 1),
(1, 1, 5,  3,  60, '手持示波器-存储位(半)',   'admin', NOW(), 1),
(1, 2, 16, 3,  30, '手持示波器-拣货位(空)',   'admin', NOW(), 1),
(1, 12, 21, 3, 30, '手持示波器-退货不良位',   'admin', NOW(), 1),

-- === SPU 4 (锂电充电钻) 规划库位 ===
(1, 1, 6,  4,  80, '锂电充电钻-存储位(满)',   'admin', NOW(), 1),
(1, 1, 7,  4,  80, '锂电充电钻-存储位(半)',   'admin', NOW(), 1),
(1, 1, 8,  4,  80, '锂电充电钻-备选空位',     'admin', NOW(), 1),
(1, 2, 17, 4,  40, '锂电充电钻-拣货位',       'admin', NOW(), 1),

-- === SPU 5 (激光测距仪) 规划库位 ===
(1, 1, 9,  5, 200, '激光测距仪-存储位(半)',   'admin', NOW(), 1),
(1, 1, 10, 5, 200, '激光测距仪-存储位(空)',   'admin', NOW(), 1),
(1, 1, 13, 5, 100, '混合库位-测距仪分位',     'admin', NOW(), 1),
(1, 2, 18, 5, 100, '激光测距仪-拣货位',       'admin', NOW(), 1),

-- === SPU 6 (热敏标签打印机) 规划库位 ===
(1, 1, 11, 6,  50, '热敏标签机-存储位(满)',   'admin', NOW(), 1),
(1, 1, 12, 6,  50, '热敏标签机-存储位(半)',   'admin', NOW(), 1),
(1, 1, 14, 6,  50, '热敏标签机-备用空位',     'admin', NOW(), 1),
(1, 2, 19, 6,  25, '热敏标签机-拣货位',       'admin', NOW(), 1);

-- ------------------------------------------------------------
-- 4. 配置梯次在库库存 (wms_inventory) 与对应流水 (wms_inventory_log)
-- ------------------------------------------------------------
-- 4.1 在库库存插入
INSERT INTO `wms_inventory`
  (`id`, `warehouse_id`, `warehouse_area_id`, `warehouse_area_loca_id`, `consignor_id`, `sku_id`, `batch_number`,
   `prod_date`, `expire_date`, `quality_status`, `total_count`, `available_count`, `locked_count`, `frozen_count`,
   `used_volume`, `used_weight`, `version`, `create_time`, `create_by`, `is_del`)
VALUES
-- [库位 1: 01-A-01-01] SPU 2 (SKU 18) 梯次1: 规划 100，在库 100 -> 已放满 (FULL, 剩余 0 件)
(1, 1, 1, 1, 0, 18, 'BAT20261101A', '2026-06-01', '2027-06-01', 1, 100, 100, 0, 0, 74900, 34000, 0, NOW(), 'admin', 1),

-- [库位 2: 01-A-01-02] SPU 2 (SKU 18) 梯次2: 规划 100，在库 45 -> 部分占用 (PARTIAL, 剩余 55 件)
(2, 1, 1, 2, 0, 18, 'BAT20261101B', '2026-07-01', '2027-07-01', 1,  45,  45, 0, 0, 33705, 15300, 0, NOW(), 'admin', 1),

-- [库位 3: 01-A-02-01] SPU 2 梯次3: 规划 100，在库 0 -> 完全空闲 (EMPTY, 剩余 100 件) 无库存行

-- [库位 13: 01-B-03-01] 混合库位：SPU 2 (SKU 18) 在库 20 件(上限50)，SPU 5 (SKU 14) 在库 50 件(上限100)
(3, 1, 1, 13, 0, 18, 'BAT20261101A', '2026-06-01', '2027-06-01', 1, 20, 20, 0, 0, 14980,  6800, 0, NOW(), 'admin', 1),
(4, 1, 1, 13, 0, 14, 'BAT20261101C', '2026-08-01', '2027-08-01', 1, 50, 50, 0, 0,  4800,  4500, 0, NOW(), 'admin', 1),

-- [库位 15: 02-A-01-01] 拣货区：SPU 2 (SKU 18) 规划 50，在库 20 -> 剩余 30 件
(5, 1, 2, 15, 0, 18, 'BAT20261101A', '2026-06-01', '2027-06-01', 1, 20, 20, 0, 0, 14980,  6800, 0, NOW(), 'admin', 1),

-- [库位 4: 01-A-02-02] SPU 3 (SKU 26) 规划 60，在库 60 -> 已放满 (FULL, 剩余 0 件)
(6, 1, 1, 4, 0, 26, 'BAT20261101A', '2026-06-01', '2027-06-01', 1, 60, 60, 0, 0, 136920, 72000, 0, NOW(), 'admin', 1),

-- [库位 5: 01-A-03-01] SPU 3 (SKU 26) 规划 60，在库 25 -> 部分占用 (PARTIAL, 剩余 35 件)
(7, 1, 1, 5, 0, 26, 'BAT20261101B', '2026-07-01', '2027-07-01', 1, 25, 25, 0, 0, 57050, 30000, 0, NOW(), 'admin', 1),

-- [库位 6: 01-A-03-02] SPU 4 (SKU 12) 规划 80，在库 80 -> 已放满 (FULL, 剩余 0 件)
(8, 1, 1, 6, 0, 12, 'BAT20261101A', '2026-06-01', '2027-06-01', 1, 80, 80, 0, 0, 277200, 116000, 0, NOW(), 'admin', 1),

-- [库位 7: 01-A-04-01] SPU 4 (SKU 12) 规划 80，在库 30 -> 部分占用 (PARTIAL, 剩余 50 件)
(9, 1, 1, 7, 0, 12, 'BAT20261101B', '2026-07-01', '2027-07-01', 1, 30, 30, 0, 0, 103950, 43500, 0, NOW(), 'admin', 1),

-- [库位 9: 01-B-01-01] SPU 5 (SKU 14) 规划 200，在库 120 -> 部分占用 (PARTIAL, 剩余 80 件)
(10, 1, 1, 9, 0, 14, 'BAT20261101A', '2026-06-01', '2027-06-01', 1, 120, 120, 0, 0, 11520, 10800, 0, NOW(), 'admin', 1),

-- [库位 11: 01-B-02-01] SPU 6 (SKU 24) 规划 50，在库 50 -> 已放满 (FULL, 剩余 0 件)
(11, 1, 1, 11, 0, 24, 'BAT20261101A', '2026-06-01', '2027-06-01', 1, 50, 50, 0, 0, 169650, 41000, 0, NOW(), 'admin', 1),

-- [库位 12: 01-B-02-02] SPU 6 (SKU 24) 规划 50，在库 10 -> 部分占用 (PARTIAL, 剩余 40 件)
(12, 1, 1, 12, 0, 24, 'BAT20261101B', '2026-07-01', '2027-07-01', 1, 10, 10, 0, 0, 33930,  8200, 0, NOW(), 'admin', 1);

-- 4.2 配套库存流水 (满足方案 7.1：每条库存 total_count = 对应流水的累计净额)
INSERT INTO `wms_inventory_log`
  (`inventory_id`, `warehouse_id`, `warehouse_area_id`, `warehouse_area_loca_id`, `consignor_id`, `sku_id`, `batch_number`,
   `biz_type`, `biz_no`, `change_count`, `before_count`, `after_count`, `remark`, `create_time`, `create_by`)
VALUES
(1,  1, 1, 1,  0, 18, 'BAT20261101A', 1, 'RK202611010001', 100, 0, 100, '期初上架初始化', NOW(), 'admin'),
(2,  1, 1, 2,  0, 18, 'BAT20261101B', 1, 'RK202611010002',  45, 0,  45, '期初上架初始化', NOW(), 'admin'),
(3,  1, 1, 13, 0, 18, 'BAT20261101A', 1, 'RK202611010003',  20, 0,  20, '期初上架初始化', NOW(), 'admin'),
(4,  1, 1, 13, 0, 14, 'BAT20261101C', 1, 'RK202611010003',  50, 0,  50, '期初上架初始化', NOW(), 'admin'),
(5,  1, 2, 15, 0, 18, 'BAT20261101A', 1, 'RK202611010004',  20, 0,  20, '期初上架初始化', NOW(), 'admin'),
(6,  1, 1, 4,  0, 26, 'BAT20261101A', 1, 'RK202611010005',  60, 0,  60, '期初上架初始化', NOW(), 'admin'),
(7,  1, 1, 5,  0, 26, 'BAT20261101B', 1, 'RK202611010006',  25, 0,  25, '期初上架初始化', NOW(), 'admin'),
(8,  1, 1, 6,  0, 12, 'BAT20261101A', 1, 'RK202611010007',  80, 0,  80, '期初上架初始化', NOW(), 'admin'),
(9,  1, 1, 7,  0, 12, 'BAT20261101B', 1, 'RK202611010008',  30, 0,  30, '期初上架初始化', NOW(), 'admin'),
(10, 1, 1, 9,  0, 14, 'BAT20261101A', 1, 'RK202611010009', 120, 0, 120, '期初上架初始化', NOW(), 'admin'),
(11, 1, 1, 11, 0, 24, 'BAT20261101A', 1, 'RK202611010010',  50, 0,  50, '期初上架初始化', NOW(), 'admin'),
(12, 1, 1, 12, 0, 24, 'BAT20261101B', 1, 'RK202611010011',  10, 0,  10, '期初上架初始化', NOW(), 'admin');

SET FOREIGN_KEY_CHECKS = 1;
