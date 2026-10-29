-- WMS 二期库存底座：演示数据（可重复执行，执行前会清空二期业务表）
-- 目的：让「实时库存 / 库存流水 / 移库移位 / 补货作业 / 盘点管理 / SN追溯」六个页面打开即有内容，
--       并且满足方案 7.1 的记账一致性：每条库存行的 total_count = 该行流水累计净额。
--
-- 数据设计（仓库 1 上海一号仓）：
--   库区1 常温存储区A（STORAGE）：L-01 ~ L-10        —— 主存储位
--                                 L-12 / L-13       —— 本脚本新建的空库位，移库落点
--   库区2 拣货区B（PICKING）    ：L-06 / L-07 / L-08  —— 补货目标位
--   库区3 收货区C（RECEIVING）  ：L-11                —— 只读展示，不放库存
--   库位 L-01 / L-02 的 allow_mix_batch=0，用于演示「批次不能混放」的校验；
--   其余库位 allow_mix_sku=0 且 allow_mix_batch=1，可放同 SKU 多批次。
--   L-12 / L-13 是本脚本新建的两个空库位，专门给移库单当落点：移库的兼容性校验要求
--   目标库位「未锁定 + 温区一致 + 混放策略允许 + 容量够」，已放别的 SKU 且
--   allow_mix_sku=0 的库位会被拦下，空库位不受混放限制。
--
-- 覆盖的场景：
--   1. 良品 / 待检 / 不良品三种质量状态（同一 SKU 同批次分三个库位）
--   2. 正常 / 临期 / 过期 / 无有效期 四种效期状态
--   3. 锁定库存与冻结库存（验证 available = total - locked - frozen）
--   4. 多货主（consignor_id 0 = 自营，其余取 wms_customer）
--   5. SN 管理的 SKU（is_sn_managed=1）配套 wms_item_sn 记录，供 SN 追溯与库存按 SN 过滤
--   6. 已执行 / 未确认 / 已取消 三种状态的移库单与补货单
--   7. 三种状态的盘点记录：待盘点 / 已盘待审核（待审批差异）/ 已确认（盘亏+盘盈已调账）

SET NAMES utf8mb4;

-- ============================================================
-- 0. 清空二期业务表（保留一期基础数据：仓库/库区/库位/SKU/商品）
-- ============================================================
DELETE FROM wms_inventory_log;
DELETE FROM wms_stocktaking_loss;
DELETE FROM wms_stocktaking_info;
DELETE FROM wms_warehouse_operations_details;
DELETE FROM wms_warehouse_operations;
DELETE FROM wms_item_sn;
DELETE FROM wms_inventory;
UPDATE wms_warehouse_area_loca SET loca_status = 1, used_volume = 0, used_weight = 0 WHERE is_del = 1;

-- ============================================================
-- 1. 库位属性微调：让一部分库位能被补货逻辑选中，并演示混放校验
-- ============================================================
-- L-01 / L-02 禁止混放批次（默认值已是 0，这里显式写死，避免被一期数据改动影响）
UPDATE wms_warehouse_area_loca SET allow_mix_sku = 0, allow_mix_batch = 0 WHERE id IN (1, 2);
-- 拣货区三个库位允许混放，保证补货能落位
UPDATE wms_warehouse_area_loca SET allow_mix_sku = 1, allow_mix_batch = 1 WHERE id IN (6, 7, 8);

-- 新建两个空库位，专供移库单当落点。已有的 L-03 放了别的 SKU 且不允许混放，
-- 当目标位会被兼容性校验拦下，所以「未确认」的移库单要落到真正的空库位才能执行。
DELETE FROM wms_warehouse_area_loca WHERE warehouse_id = 1 AND loca_code IN ('L-12', 'L-13');
INSERT INTO wms_warehouse_area_loca
  (warehouse_id, warehouse_area_id, loca_code, local_volume, local_bearing_capacity,
   storage_condition, loca_status, allow_mix_sku, allow_mix_batch,
   used_volume, used_weight, create_time, create_by, is_del)
VALUES (1, 1, 'L-12', 1200000, 500000, NULL, 1, 0, 1, 0, 0, NOW(), 'admin', 1);
SET @loca_moved = LAST_INSERT_ID();
INSERT INTO wms_warehouse_area_loca
  (warehouse_id, warehouse_area_id, loca_code, local_volume, local_bearing_capacity,
   storage_condition, loca_status, allow_mix_sku, allow_mix_batch,
   used_volume, used_weight, create_time, create_by, is_del)
VALUES (1, 1, 'L-13', 1200000, 500000, NULL, 1, 0, 1, 0, 0, NOW(), 'admin', 1);
SET @loca_pending = LAST_INSERT_ID();

-- ============================================================
-- 2. 实时库存
--    恒等式：available_count = total_count - locked_count - frozen_count
--    uk_inv_dim = (库位, SKU, 批次, 货主, 质量状态)，下面每行都保证五维唯一
-- ============================================================
INSERT INTO wms_inventory
  (warehouse_id, warehouse_area_id, warehouse_area_loca_id, consignor_id, sku_id, batch_number,
   prod_date, expire_date, quality_status, total_count, available_count, locked_count, frozen_count,
   used_volume, used_weight, version, create_time, create_by, is_del)
VALUES
-- ---- 存储区 L-01：数字万用表（SKU 18），三个批次，效期状态各不相同 ----
-- 批次 B2401：效期 2027-06-30 → 效期状态「正常」
-- 这张行稍后会被 5.1 的「已执行移库」扣掉 30 件（120 → 90），落到 L-12
(1, 1, 1, 0, 18, 'B2401', '2026-06-30', '2027-06-30', 1, 120, 120,  0,  0, 0, 0, 0, NOW(), 'admin', 1),
-- 批次 B2402：效期 2026-12-15（距今约 47 天）→「临期」（默认阈值 30 天，这里按近效期数据准备）
(1, 1, 1, 0, 18, 'B2402', '2026-01-15', '2026-11-25', 1,  80,  70, 10,  0, 0, 0, 0, NOW(), 'admin', 1),
-- 批次 B2403：效期 2026-10-01 已过期 →「过期」，同时演示冻结库存
(1, 1, 1, 0, 18, 'B2403', '2025-10-01', '2026-10-01', 1,  50,  42,  0,  8, 0, 0, 0, NOW(), 'admin', 1),

-- ---- 存储区 L-02：手持示波器（SKU 10、11），无有效期 →「无有效期」----
(1, 1, 2, 0, 10, 'B2501', NULL, NULL, 1, 240, 240, 0, 0, 0, 0, 0, NOW(), 'admin', 1),
(1, 1, 2, 0, 11, 'B2501', NULL, NULL, 1, 160, 160, 0, 0, 0, 0, 0, NOW(), 'admin', 1),

-- ---- 存储区 L-03：锂电充电钻（SKU 12），待检质量状态（不良品不入存储区）----
(1, 1, 3, 0, 12, 'B2601', '2026-08-01', '2027-08-01', 2,  60,  60, 0, 0, 0, 0, 0, NOW(), 'admin', 1),
-- ---- 存储区 L-04：锂电充电钻（SKU 12）不良品，展示质量状态过滤 ----
(1, 1, 4, 0, 12, 'B2601', '2026-08-01', '2027-08-01', 3,  15,  15, 0, 0, 0, 0, 0, NOW(), 'admin', 1),

-- ---- 存储区 L-05：激光测距仪（SKU 14），带一个外部货主 ----
(1, 1, 5, 0, 14, 'B2701', '2026-09-10', '2027-09-10', 1, 300, 300, 0, 0, 0, 0, 0, NOW(), 'admin', 1),

-- ---- 存储区 L-09：热敏标签打印机（SKU 16），大数量，用于补货取货 ----
(1, 1, 9, 0, 16, 'B2801', '2026-09-20', '2027-09-20', 1, 500, 500, 0, 0, 0, 0, 0, NOW(), 'admin', 1),
-- ---- 存储区 L-10：冷链疫苗（SKU 20），SN 管理商品，效期 2026-11-15 → 临期 ----
(1, 1, 10, 0, 20, 'B2901', '2026-07-01', '2026-11-15', 1,  30,  30, 0, 0, 0, 0, 0, NOW(), 'admin', 1),

-- ---- 拣货区 L-06：数字万用表，拣货区库存低于 min_stock（50），可触发补货 ----
(1, 2, 6, 0, 18, 'B2401', '2026-06-30', '2027-06-30', 1,  20,  20, 0, 0, 0, 0, 0, NOW(), 'admin', 1),
-- ---- 拣货区 L-07：热敏标签打印机，拣货区库存低于 min_stock，可触发补货 ----
(1, 2, 7, 0, 16, 'B2801', '2026-09-20', '2027-09-20', 1,  35,  35, 0, 0, 0, 0, 0, NOW(), 'admin', 1),
-- ---- 拣货区 L-08：手持示波器（SKU 10），数量充足，不触发补货 ----
(1, 2, 8, 0, 10, 'B2501', NULL, NULL, 1, 180, 180, 0, 0, 0, 0, 0, NOW(), 'admin', 1);

-- ============================================================
-- 3. 库存流水
--    口径与 WmsInventoryServiceImpl.writeLog 一致：
--      change_count = 本次物理库存变动量；
--      before_count / after_count = 变动前后的「可用」数量
--      （可用 = 总量 - 锁定 - 冻结，锁定/冻结本身也各记一条变动量为 0 的流水）。
--    这样「库存合计 = 流水累计净额」成立，且锁定/冻结在流水页上有据可查。
--    biz_no 用可读的演示单号，便于在页面上串联。
-- ============================================================
-- 3.1 期初入库上架：入库当时还没有锁定/冻结，故 after = total
INSERT INTO wms_inventory_log
  (inventory_id, biz_type, biz_no, warehouse_id, warehouse_area_id, warehouse_area_loca_id,
   consignor_id, sku_id, batch_number, change_count, before_count, after_count, remark, create_time, create_by)
SELECT
  i.id,
  1,                                            -- biz_type=1 入库上架
  CONCAT('DEMO-IN-', DATE_FORMAT(NOW(), '%Y%m%d'), '-', LPAD(i.id, 3, '0')),
  i.warehouse_id, i.warehouse_area_id, i.warehouse_area_loca_id,
  i.consignor_id, i.sku_id, i.batch_number,
  i.total_count, 0, i.total_count,
  '演示数据：期初入库上架',
  DATE_SUB(NOW(), INTERVAL 3 DAY),
  'admin'
FROM wms_inventory i;

-- 3.2 锁定：变动数量为 0，只把可用数扣下去
INSERT INTO wms_inventory_log
  (inventory_id, biz_type, biz_no, warehouse_id, warehouse_area_id, warehouse_area_loca_id,
   consignor_id, sku_id, batch_number, change_count, before_count, after_count, remark, create_time, create_by)
SELECT i.id, 9, CONCAT('DEMO-LOCK-', LPAD(i.id, 3, '0')),   -- biz_type=9 锁定
       i.warehouse_id, i.warehouse_area_id, i.warehouse_area_loca_id,
       i.consignor_id, i.sku_id, i.batch_number,
       0, i.total_count, i.total_count - i.locked_count,
       '演示数据：出库预占锁定',
       DATE_SUB(NOW(), INTERVAL 2 DAY), 'admin'
FROM wms_inventory i
WHERE i.is_del = 1 AND i.locked_count > 0;

-- 3.3 冻结：再往下扣一段可用
INSERT INTO wms_inventory_log
  (inventory_id, biz_type, biz_no, warehouse_id, warehouse_area_id, warehouse_area_loca_id,
   consignor_id, sku_id, batch_number, change_count, before_count, after_count, remark, create_time, create_by)
SELECT i.id, 7, CONCAT('DEMO-FREEZE-', LPAD(i.id, 3, '0')),  -- biz_type=7 冻结
       i.warehouse_id, i.warehouse_area_id, i.warehouse_area_loca_id,
       i.consignor_id, i.sku_id, i.batch_number,
       0, i.total_count - i.locked_count, i.available_count,
       '演示数据：过期批次冻结',
       DATE_SUB(NOW(), INTERVAL 1 DAY), 'admin'
FROM wms_inventory i
WHERE i.is_del = 1 AND i.frozen_count > 0;

-- ============================================================
-- 4. SN 记录（仅 SN 管理商品 SKU 20 冷链疫苗，L-10 库位 30 件）
--    SN 与库存行按 (库位, SKU, 批次) 对齐，库存页的 SN 过滤才能命中。
-- ============================================================
INSERT INTO wms_item_sn
  (sn_code, sku_id, batch_number, warehouse_id, warehouse_area_loca_id, quality_status,
   status, create_time, create_by, is_del)
SELECT
  CONCAT('VAC-B2901-', LPAD(n.seq, 4, '0')),
  20, 'B2901', 1, 10, 1,
  2,                                            -- status=2 在库良品
  DATE_SUB(NOW(), INTERVAL 3 DAY), 'admin', 1
FROM (
  SELECT 1 AS seq UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
  UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
  UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
  UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
  UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25
  UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
) n;

-- ============================================================
-- 5. 库内作业单
--    operation_type: 1 移库移位 / 2 补货
--    status:        1 未确认 / 2 已执行 / 3 已取消
-- ============================================================

-- ---- 5.1 移库单（已执行）：L-01 B2401 万用表 30 件 → L-12 ----
--      单据、库存、流水三处闭环，等价于 WarehouseOperationsService.execute() 的效果：
--      源库位扣 30（biz_type 3 移库移出），目标库位建行 +30（biz_type 4 移库移入）。
INSERT INTO wms_warehouse_operations
  (job_code, warehouse_id, category, operation_type, source_type, status,
   emp_id, remark, create_time, update_time, create_by, is_del)
VALUES ('MV20261026000101', 1, 1, 1, 1, 2, NULL, '演示：已执行的移库单（L-01 → L-12）',
        DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'admin', 1);
SET @op_move_done = LAST_INSERT_ID();

INSERT INTO wms_warehouse_operations_details
  (warehouse_operations_id, sku_id, batch_number, consignor_id, quality_status,
   source_count, target_count, source_warehouse_area_id, source_warehouse_area_loca_id,
   target_warehouse_id, target_warehouse_area_id, target_warehouse_area_loca_id, category, price)
VALUES (@op_move_done, 18, 'B2401', 0, 1, 30, 30, 1, 1, 1, 1, @loca_moved, 1, 0);

-- 移出：L-01 B2401 从 120 扣到 90
INSERT INTO wms_inventory_log
  (inventory_id, biz_type, biz_no, warehouse_id, warehouse_area_id, warehouse_area_loca_id,
   consignor_id, sku_id, batch_number, change_count, before_count, after_count, remark, create_time, create_by)
SELECT i.id, 3, 'MV20261026000101', i.warehouse_id, i.warehouse_area_id, i.warehouse_area_loca_id,
       i.consignor_id, i.sku_id, i.batch_number,
       -30, i.available_count, i.available_count - 30, CONCAT('移库移出到库位ID ', @loca_moved),
       DATE_SUB(NOW(), INTERVAL 2 DAY), 'admin'
FROM wms_inventory i
WHERE i.warehouse_area_loca_id = 1 AND i.sku_id = 18 AND i.batch_number = 'B2401' AND i.is_del = 1;

UPDATE wms_inventory
SET total_count = total_count - 30, available_count = available_count - 30,
    update_time = DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE warehouse_area_loca_id = 1 AND sku_id = 18 AND batch_number = 'B2401' AND is_del = 1;

-- 移入：L-12 新建一行（生产/有效期沿用源库存行），再记一条 +30
INSERT INTO wms_inventory
  (warehouse_id, warehouse_area_id, warehouse_area_loca_id, consignor_id, sku_id, batch_number,
   prod_date, expire_date, quality_status, total_count, available_count, locked_count, frozen_count,
   used_volume, used_weight, version, create_time, update_time, create_by, is_del)
SELECT 1, 1, @loca_moved, 0, 18, 'B2401', '2026-06-30', '2027-06-30', 1, 30, 30, 0, 0,
       0, 0, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'admin', 1;
SET @inv_moved = LAST_INSERT_ID();

INSERT INTO wms_inventory_log
  (inventory_id, biz_type, biz_no, warehouse_id, warehouse_area_id, warehouse_area_loca_id,
   consignor_id, sku_id, batch_number, change_count, before_count, after_count, remark, create_time, create_by)
VALUES (@inv_moved, 4, 'MV20261026000101', 1, 1, @loca_moved, 0, 18, 'B2401',
        30, 0, 30, '移库移入，来源库位ID 1', DATE_SUB(NOW(), INTERVAL 2 DAY), 'admin');

-- ---- 5.2 移库单（未确认）：L-03 待检示波器 20 件 → L-13，待人工执行 ----
--      来源 L-03 是非盘点锁定的库位，目标是空库位，点「执行」可以直接成功。
INSERT INTO wms_warehouse_operations
  (job_code, warehouse_id, category, operation_type, source_type, status,
   emp_id, remark, create_time, update_time, create_by, is_del)
VALUES ('MV20261029000102', 1, 1, 1, 1, 1, NULL, '演示：未确认的移库单（L-03 → L-13）',
        NOW(), NOW(), 'admin', 1);
SET @op_move_pending = LAST_INSERT_ID();

INSERT INTO wms_warehouse_operations_details
  (warehouse_operations_id, sku_id, batch_number, consignor_id, quality_status,
   source_count, target_count, source_warehouse_area_id, source_warehouse_area_loca_id,
   target_warehouse_id, target_warehouse_area_id, target_warehouse_area_loca_id, category, price)
VALUES (@op_move_pending, 12, 'B2601', 0, 2, 20, 20, 1, 3, 1, 1, @loca_pending, 1, 0);

-- ---- 5.3 移库单（已取消）：留痕展示 ----
INSERT INTO wms_warehouse_operations
  (job_code, warehouse_id, category, operation_type, source_type, status,
   emp_id, remark, create_time, update_time, create_by, is_del)
VALUES ('MV20261028000103', 1, 1, 1, 1, 3, NULL, '演示：已取消的移库单',
        DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'admin', 1);
SET @op_move_cancel = LAST_INSERT_ID();

INSERT INTO wms_warehouse_operations_details
  (warehouse_operations_id, sku_id, batch_number, consignor_id, quality_status,
   source_count, target_count, source_warehouse_area_id, source_warehouse_area_loca_id,
   target_warehouse_id, target_warehouse_area_id, target_warehouse_area_loca_id, category, price)
VALUES (@op_move_cancel, 16, 'B2801', 0, 1, 10, 10, 1, 9, 1, 2, 7, 1, 0);

-- ---- 5.4 补货单（未确认）：存储区 L-09 → 拣货区 L-07，热敏标签打印机 ----
INSERT INTO wms_warehouse_operations
  (job_code, warehouse_id, category, operation_type, source_type, status,
   emp_id, remark, create_time, update_time, create_by, is_del)
VALUES ('RP20261029000201', 1, 2, 2, 3, 1, NULL, '系统补货：拣货区低于补货下限时自动生成',
        NOW(), NOW(), 'admin', 1);
SET @op_replenish = LAST_INSERT_ID();

INSERT INTO wms_warehouse_operations_details
  (warehouse_operations_id, sku_id, batch_number, consignor_id, quality_status,
   source_count, target_count, source_warehouse_area_id, source_warehouse_area_loca_id,
   target_warehouse_id, target_warehouse_area_id, target_warehouse_area_loca_id, category, price)
VALUES (@op_replenish, 16, 'B2801', 0, 1, 65, 65, 1, 9, 1, 2, 7, 1, 0);

-- ============================================================
-- 6. 盘点记录
--    status:       1 待盘点 / 2 已盘待审核 / 3 已确认
--    audit_status: 0 无差异 / 1 待审批 / 2 已通过 / 3 已驳回
--    loss_type:    1 盘亏 / 2 盘盈
-- ============================================================

-- ---- 6.1 盘点批次 #1：待盘点（已生成、库位已锁定，尚未录入实盘）----
SET @batch_pending = CONCAT('ST', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 HOUR), '%Y%m%d%H%i%s'));
INSERT INTO wms_stocktaking_info
  (batch_no, category, target_warehouse_id, target_warehouse_area_id, target_warehouse_area_loca_id,
   sku_id, consignor_id, batch_number, expire_date, quality_status,
   book_count, correct_count, discrepancy_count, audit_status, status,
   create_time, create_by, is_del)
VALUES (@batch_pending, 1, 1, 2, 8, 10, 0, 'B2501', NULL, 1, 180, NULL, NULL, 0, 1,
        DATE_SUB(NOW(), INTERVAL 1 HOUR), 'admin', 1);

-- ---- 6.2 盘点批次 #2：已盘待审核，账面 240 实盘 236 → 差 -4，待审批 ----
SET @batch_audit = CONCAT('ST', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 30 MINUTE), '%Y%m%d%H%i%s'));
INSERT INTO wms_stocktaking_info
  (batch_no, category, target_warehouse_id, target_warehouse_area_id, target_warehouse_area_loca_id,
   sku_id, consignor_id, batch_number, expire_date, quality_status,
   book_count, correct_count, discrepancy_count, audit_status, status,
   create_time, create_by, is_del)
VALUES (@batch_audit, 1, 1, 1, 2, 10, 0, 'B2501', NULL, 1, 240, 236, -4, 1, 2,
        DATE_SUB(NOW(), INTERVAL 30 MINUTE), 'admin', 1);

-- ---- 6.3 盘点批次 #3：已确认（盘亏已调账 + 盘盈已调账），展示结案后的完整链路 ----
--      库位 L-05 激光测距仪账面 300，实盘 296 → 盘亏 4，审批通过并结案
SET @batch_done = CONCAT('ST', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 2 DAY), '%Y%m%d%H%i%s'));
INSERT INTO wms_stocktaking_info
  (batch_no, category, target_warehouse_id, target_warehouse_area_id, target_warehouse_area_loca_id,
   sku_id, consignor_id, batch_number, expire_date, quality_status,
   book_count, correct_count, discrepancy_count, audit_status, audit_by, audit_time, status,
   create_time, create_by, is_del)
VALUES (@batch_done, 2, 1, 1, 5, 14, 0, 'B2701', '2027-09-10', 1, 300, 296, -4, 2, 'admin',
        DATE_SUB(NOW(), INTERVAL 2 DAY), 3, DATE_SUB(NOW(), INTERVAL 2 DAY), 'admin', 1);
SET @stk_done = LAST_INSERT_ID();

INSERT INTO wms_stocktaking_loss
  (stocktaking_info, loss_type, loss_category, loss_count, batch_number, create_time, create_by)
VALUES (@stk_done, 1, 1, 4, 'B2701', DATE_SUB(NOW(), INTERVAL 2 DAY), 'admin');

-- 对应的盘点调账流水（与 6.3 的盘亏一致）
INSERT INTO wms_inventory_log
  (inventory_id, biz_type, biz_no, warehouse_id, warehouse_area_id, warehouse_area_loca_id,
   consignor_id, sku_id, batch_number, change_count, before_count, after_count, remark, create_time, create_by)
SELECT i.id, 6, @batch_done, i.warehouse_id, i.warehouse_area_id, i.warehouse_area_loca_id,
       i.consignor_id, i.sku_id, i.batch_number,
       -4, 300, 296, '演示：盘点差异调账（盘亏 4）',
       DATE_SUB(NOW(), INTERVAL 2 DAY), 'admin'
FROM wms_inventory i
WHERE i.warehouse_area_loca_id = 5 AND i.sku_id = 14 AND i.batch_number = 'B2701' AND i.is_del = 1;

-- 盘亏后 L-05 的账面数从 300 调到 296
UPDATE wms_inventory
SET total_count = 296, available_count = 296, update_time = DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE warehouse_area_loca_id = 5 AND sku_id = 14 AND batch_number = 'B2701' AND is_del = 1;

-- ---- 6.4 盘点批次 #4：已确认（盘盈），账面 20 实盘 23 → 盘盈 3 ----
SET @batch_surplus = CONCAT('ST', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 DAY), '%Y%m%d%H%i%s'));
INSERT INTO wms_stocktaking_info
  (batch_no, category, target_warehouse_id, target_warehouse_area_id, target_warehouse_area_loca_id,
   sku_id, consignor_id, batch_number, expire_date, quality_status,
   book_count, correct_count, discrepancy_count, audit_status, audit_by, audit_time, status,
   create_time, create_by, is_del)
VALUES (@batch_surplus, 1, 1, 2, 6, 18, 0, 'B2401', '2027-06-30', 1, 20, 23, 3, 2, 'admin',
        DATE_SUB(NOW(), INTERVAL 1 DAY), 3, DATE_SUB(NOW(), INTERVAL 1 DAY), 'admin', 1);
SET @stk_surplus = LAST_INSERT_ID();

INSERT INTO wms_stocktaking_loss
  (stocktaking_info, loss_type, loss_category, loss_count, batch_number, create_time, create_by)
VALUES (@stk_surplus, 2, 1, 3, 'B2401', DATE_SUB(NOW(), INTERVAL 1 DAY), 'admin');

INSERT INTO wms_inventory_log
  (inventory_id, biz_type, biz_no, warehouse_id, warehouse_area_id, warehouse_area_loca_id,
   consignor_id, sku_id, batch_number, change_count, before_count, after_count, remark, create_time, create_by)
SELECT i.id, 6, @batch_surplus, i.warehouse_id, i.warehouse_area_id, i.warehouse_area_loca_id,
       i.consignor_id, i.sku_id, i.batch_number,
       3, 20, 23, '演示：盘点差异调账（盘盈 3）',
       DATE_SUB(NOW(), INTERVAL 1 DAY), 'admin'
FROM wms_inventory i
WHERE i.warehouse_area_loca_id = 6 AND i.sku_id = 18 AND i.batch_number = 'B2401' AND i.is_del = 1;

UPDATE wms_inventory
SET total_count = 23, available_count = 23, update_time = DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE warehouse_area_loca_id = 6 AND sku_id = 18 AND batch_number = 'B2401' AND is_del = 1;

-- ============================================================
-- 7. 库位状态与占用回填（页面上的库位状态列会用到）
-- ============================================================
-- 有库存的库位置为占用，盘点未结案的库位置为盘点锁定
UPDATE wms_warehouse_area_loca l
SET l.loca_status = 2
WHERE EXISTS (SELECT 1 FROM wms_inventory i
              WHERE i.warehouse_area_loca_id = l.id AND i.total_count > 0 AND i.is_del = 1);

-- 6.1 / 6.2 两个未结案批次涉及的库位（L-08、L-02）置为盘点锁定
UPDATE wms_warehouse_area_loca SET loca_status = 3 WHERE id IN (2, 8);

-- ============================================================
-- 8. 自检输出
-- ============================================================
SELECT '库存行数' AS item, COUNT(*) AS val FROM wms_inventory WHERE is_del = 1
UNION ALL SELECT '流水行数', COUNT(*) FROM wms_inventory_log
UNION ALL SELECT 'SN 数量', COUNT(*) FROM wms_item_sn WHERE is_del = 1
UNION ALL SELECT '作业单数', COUNT(*) FROM wms_warehouse_operations WHERE is_del = 1
UNION ALL SELECT '盘点记录', COUNT(*) FROM wms_stocktaking_info WHERE is_del = 1
UNION ALL SELECT '损溢记录', COUNT(*) FROM wms_stocktaking_loss
UNION ALL SELECT '恒等式违例(可用≠总量-锁定-冻结)',
    COUNT(*) FROM wms_inventory
    WHERE is_del = 1 AND available_count <> total_count - locked_count - frozen_count
UNION ALL SELECT '对账违例(库存≠流水净额)',
    COUNT(*) FROM wms_inventory i
    WHERE i.is_del = 1
      AND i.total_count <> (SELECT IFNULL(SUM(l.change_count), 0) FROM wms_inventory_log l WHERE l.inventory_id = i.id)
UNION ALL SELECT '可用链断裂(末条流水变动后可用≠当前可用)',
    COUNT(*) FROM wms_inventory i
    WHERE i.is_del = 1
      AND (SELECT l.after_count FROM wms_inventory_log l
           WHERE l.inventory_id = i.id ORDER BY l.create_time DESC, l.id DESC LIMIT 1) <> i.available_count;
