package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description: 移库 / 补货明细行入参
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@Schema(description = "移库/补货明细行")
public class WmsOperationDetailForm {

    /**SKU ID*/
    @Schema(description = "SKU ID")
    private Long skuId;

    /**批次号*/
    @Schema(description = "批次号")
    private String batchNumber;

    /**货主ID*/
    @Schema(description = "货主ID")
    private Long consignorId;

    /**质量状态(1良品 2待检 3不良品)*/
    @Schema(description = "质量状态(1良品 2待检 3不良品)")
    private Integer qualityStatus;

    /**移出库位ID*/
    @Schema(description = "移出库位ID")
    private Integer sourceWarehouseAreaLocaId;

    /**移入库位ID（补货时由后端按 FEFO 推荐，可留空）*/
    @Schema(description = "移入库位ID")
    private Integer targetWarehouseAreaLocaId;

    /**数量*/
    @Schema(description = "数量")
    private Integer count;
}
