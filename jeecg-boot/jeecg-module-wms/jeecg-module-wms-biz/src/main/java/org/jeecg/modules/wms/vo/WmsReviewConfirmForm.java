package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 出库复核确认表单
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "出库复核确认表单")
public class WmsReviewConfirmForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 出库单ID */
    @NotNull(message = "出库单ID不能为空")
    @Schema(description = "出库单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long outboundOrderId;

    /** 整单复核结果(1复核通过 2复核异常) */
    @Schema(description = "整单复核结果(1复核通过 2复核异常)")
    private Integer reviewStatus;

    /** 复核说明/意见（异常时必填） */
    @Schema(description = "复核说明/意见")
    private String reviewRemark;

    /** 复核明细清单（选填，整单快速复核可不传） */
    @Schema(description = "复核明细清单")
    private List<WmsReviewLineItem> reviewLines;

    @Data
    @Schema(description = "复核行")
    public static class WmsReviewLineItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @NotNull(message = "明细ID不能为空")
        @Schema(description = "明细ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long detailId;

        /** 复核合格数量 */
        @NotNull(message = "复核数量不能为空")
        @Schema(description = "复核数量", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer reviewCount;

        /** 复核状态(1通过 2不通过) */
        @NotNull(message = "复核状态不能为空")
        @Schema(description = "复核状态(1通过 2不通过)", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer reviewStatus;
    }
}
