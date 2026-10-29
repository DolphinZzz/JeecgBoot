package org.jeecg.modules.wms.vo;

import lombok.Data;

/**
 * @Description: SN 追溯查询条件
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
public class WmsItemSnQuery {

    /**SN 序列号（模糊）*/
    private String snCode;

    /**SKU ID*/
    private Long skuId;

    /**批次号（精确）*/
    private String batchNumber;

    /**仓库ID*/
    private Integer warehouseId;

    /**库位ID*/
    private Integer warehouseAreaLocaId;

    /**MES 生产工单号*/
    private String mesWorkOrderNo;

    /**SN 状态*/
    private Integer status;

    /**入库单ID*/
    private Long entryOrderId;

    /**出库单ID*/
    private Long outboundOrderId;
}
