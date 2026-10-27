-- 供应商、商品类别主键改为数据库自增。已有数据保留，自增起点为当前最大 id + 1。

ALTER TABLE wms_supplier
    MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT COMMENT '主键';

ALTER TABLE wms_item_category
    MODIFY COLUMN `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID';
