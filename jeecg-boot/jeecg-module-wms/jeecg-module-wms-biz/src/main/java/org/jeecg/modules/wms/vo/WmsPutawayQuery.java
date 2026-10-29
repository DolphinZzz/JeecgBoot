package org.jeecg.modules.wms.vo;

import lombok.Data;

/**
 * @Description: 上架记录查询条件
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
public class WmsPutawayQuery {

    /**入库单号（模糊）*/
    private String entryCode;

    /**仓库ID*/
    private Integer warehouseId;

    /**内 = 库区ID*/
    private Integer warehouseAreaId;

    /**上架库位ID*/
    private Integer warehouseAreaLocaId;

    /**SKU ID*/
    private Long skuId;

    /**批次号（模糊）*/
    private String batchNumber;

    /**上架状态(1未上架 2已上架)*/
    private Integer status;

    /**上架策略(FIXED/ABC/NEAREST)*/
    private String strategyUsed;

    /**是否只看偏离推荐的记录*/
    private Boolean onlyDeviate;
}
