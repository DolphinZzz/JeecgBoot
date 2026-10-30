-- WMS 三期（入库闭环）：库位补「位号」，商品 SKU 字典补「编码 + 商品名」
--
-- 1) 库位只有 巷道号 / 货架号 / 层号，作业员在实际货架上找不到具体格子；
--    补一个 位号（position_number），与前三者组成 巷-架-层-位 的完整坐标。
-- 2) 表字典 `table,text,code` 只能取一个显示列，而 wms_item_sku 只有 code，
--    商品名在 wms_item_spu.title，于是全站 SKU 都只显示一串编号，非常不直观。
--    建一个视图把两者拼成 text，字典统一改成 `wms_item_sku_v,text,id`。
--    视图携带 is_del = 1 过滤，软删的 SKU 不会再出现在下拉里。

SET @sql := (SELECT IF(COUNT(*)=0,
  'ALTER TABLE `wms_warehouse_area_loca` ADD COLUMN `position_number` varchar(30) NOT NULL DEFAULT '''' COMMENT ''位号'' AFTER `layer_number`',
  'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='wms_warehouse_area_loca' AND column_name='position_number');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- SKU 字典视图：text = 「编码 商品名」，id 为 SKU 主键，供表字典取用
CREATE OR REPLACE VIEW `wms_item_sku_v` AS
SELECT
  s.id AS id,
  CONCAT(IFNULL(s.code, ''),
         CASE WHEN IFNULL(p.title, '') = '' THEN '' ELSE CONCAT(' ', p.title) END) AS text,
  s.code AS code,
  IFNULL(p.title, '') AS title,
  s.spu_id AS spu_id
FROM wms_item_sku s
LEFT JOIN wms_item_spu p ON p.id = s.spu_id AND p.is_del = 1
WHERE s.is_del = 1;

-- 表字典白名单：非 dev 模式下访问未登记的表会直接拒绝
INSERT INTO sys_table_white_list(id, table_name, field_name, status, create_by, create_time)
SELECT '179350000000009', 'wms_item_sku_v', 'id,text,code,title,spu_id', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_table_white_list WHERE table_name = 'wms_item_sku_v');
