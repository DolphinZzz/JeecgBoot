-- 商品测量单位挂在 SPU 上：长度（含宽、高）、体积、重量各一个。不使用 SKU 的 unit 冗余列。

ALTER TABLE wms_item_spu
    ADD COLUMN weight_unit varchar(10) NULL COMMENT '重量单位 mg/g/kg' AFTER supplier_id,
    ADD COLUMN length_unit varchar(10) NULL COMMENT '长度单位 mm/cm/dm/m' AFTER weight_unit,
    ADD COLUMN volume_unit varchar(10) NULL COMMENT '体积单位' AFTER length_unit;

UPDATE wms_item_spu
SET weight_unit = 'g', length_unit = 'mm', volume_unit = 'cm³'
WHERE title IN ('数字万用表', '手持示波器', '锂电充电钻', '激光测距仪', '热敏标签打印机')
  AND is_del = 1
  AND weight_unit IS NULL;
