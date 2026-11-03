package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description: 发运出库确认表单
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "发货出库确认表单")
public class WmsShipConfirmForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 出库单ID */
    @NotNull(message = "出库单ID不能为空")
    @Schema(description = "出库单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long outboundOrderId;

    /** 承运商ID */
    @Schema(description = "承运商ID")
    private Long logisticsCompanyId;

    /** 最终物流运单号 */
    @Schema(description = "物流运单号")
    private String logisticsNumber;

    /** 物流运费(元) */
    @Schema(description = "物流运费(元)")
    private Double freightFee;
}
