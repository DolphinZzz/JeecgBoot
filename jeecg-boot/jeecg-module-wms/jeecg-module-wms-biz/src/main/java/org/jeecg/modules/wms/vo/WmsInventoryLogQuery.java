package org.jeecg.modules.wms.vo;

import lombok.Data;

/**
 * @Description: 库存流水查询条件
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
public class WmsInventoryLogQuery {

    /**库存行ID（从库存中心穿透过来时必填）*/
    private Long inventoryId;

    /**仓库ID*/
    private Integer warehouseId;

    /**库位ID*/
    private Integer warehouseAreaLocaId;

    /**SKU ID*/
    private Long skuId;

    /**批次号（精确）*/
    private String batchNumber;

    /**业务类型*/
    private Integer bizType;

    /**关联单号（模糊）*/
    private String bizNo;

    /**变动时间下限，yyyy-MM-dd HH:mm:ss*/
    private String beginTime;

    /**变动时间上限，yyyy-MM-dd HH:mm:ss*/
    private String endTime;
}
