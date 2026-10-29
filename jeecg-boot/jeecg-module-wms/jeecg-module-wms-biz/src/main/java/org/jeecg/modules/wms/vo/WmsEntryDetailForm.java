package org.jeecg.modules.wms.vo;

import java.math.BigDecimal;
import java.util.Date;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description: 入库单明细入参
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@Schema(description = "入库单明细")
public class WmsEntryDetailForm {

    /**SKU ID*/
    @Schema(description = "SKU ID")
    private Long skuId;

    /**计划数量*/
    @Schema(description = "计划数量")
    private Integer count;

    /**供应商ID*/
    @Schema(description = "供应商ID")
    private Long supplierId;

    /**单价（元），落库按分存储*/
    @Schema(description = "单价(元)")
    private BigDecimal price;

    /**生产日期*/
    @Schema(description = "生产日期")
    private Date prodDate;

    /**有效期至，留空按 SPU 保质期天数推算*/
    @Schema(description = "有效期至")
    private Date expireDate;

    /**预测到达时间*/
    @Schema(description = "预测到达时间")
    private Date predictedArrivalTime;
}
