package org.jeecg.modules.wms.vo;

import lombok.Data;

/**
 * @Description: 入库单查询条件
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
public class WmsEntryQuery {

    /**入库单号（模糊）*/
    private String entryCode;

    /**入库类型(1采购入库 2MES生产入库 3销退入库 4调拨入库)*/
    private Integer entryType;

    /**仓库ID*/
    private Integer warehouseId;

    /**货主ID*/
    private Long consignorId;

    /**批次号（模糊）*/
    private String batchNumber;

    /**MES生产工单号（模糊）*/
    private String mesWorkOrderNo;

    /**上游ERP采购单号（模糊）*/
    private String erpPoNo;

    /**入库单状态(10待到货 20收货清点中 30待质检 40质检完成待上架 50上架中 60已完成 90异常挂起 99已取消)*/
    private Integer status;

    /**是否只看未完成的单据（status 非 60/99），默认 false 看全部*/
    private Boolean onlyOpen;
}
