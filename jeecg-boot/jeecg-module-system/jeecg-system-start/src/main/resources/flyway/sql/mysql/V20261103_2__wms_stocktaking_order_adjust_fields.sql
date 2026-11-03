-- ====================================================================
-- WMS 盘库单表扩充：账面总金额、盘盈/盘亏件数金额及出入库调账单号追踪
-- 日期: 2026-11-03
-- ====================================================================

ALTER TABLE wms_stocktaking_order
    ADD COLUMN total_book_amount DECIMAL(15,2) DEFAULT 0.00 COMMENT '账面总金额' AFTER total_actual_amount,
    ADD COLUMN profit_count INT DEFAULT 0 COMMENT '盘盈总数量' AFTER total_discrepancy_count,
    ADD COLUMN profit_amount DECIMAL(15,2) DEFAULT 0.00 COMMENT '盘盈总金额' AFTER profit_count,
    ADD COLUMN loss_count INT DEFAULT 0 COMMENT '盘亏总数量' AFTER profit_amount,
    ADD COLUMN loss_amount DECIMAL(15,2) DEFAULT 0.00 COMMENT '盘亏总金额' AFTER loss_count,
    ADD COLUMN adjust_in_status INT DEFAULT 0 COMMENT '盘盈入库状态(0未生成 1已生成 2无需生成)' AFTER status,
    ADD COLUMN adjust_in_order_no VARCHAR(50) NULL COMMENT '生成的盘盈入库单号' AFTER adjust_in_status,
    ADD COLUMN adjust_out_status INT DEFAULT 0 COMMENT '盘亏出库状态(0未生成 1已生成 2无需生成)' AFTER adjust_in_order_no,
    ADD COLUMN adjust_out_order_no VARCHAR(50) NULL COMMENT '生成的盘亏出库单号' AFTER adjust_out_status;
