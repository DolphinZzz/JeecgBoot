package org.jeecg.modules.wms.vo;

import lombok.Data;

/**
 * @Description: 质检单查询条件
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
public class WmsQualityCheckQuery {

    /**质检单号（模糊）*/
    private String qcCode;

    /**入库单号（模糊）*/
    private String entryCode;

    /**仓库ID*/
    private Integer warehouseId;

    /**SKU ID*/
    private Long skuId;

    /**批次号（模糊）*/
    private String batchNumber;

    /**MES生产工单号（模糊）*/
    private String mesWorkOrderNo;

    /**质检状态(1待质检 2质检中 3合格 4不合格 5部分合格)*/
    private Integer status;

    /**是否只看未出结论的（status 为 1/2），默认 false 看全部*/
    private Boolean onlyPending;
}
