package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 库存分配结果VO
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "库存分配结果VO")
public class WmsOutboundAllocateResultVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 出库单ID */
    @Schema(description = "出库单ID")
    private Long outboundOrderId;

    /** 出库单号 */
    @Schema(description = "出库单号")
    private String outboundCode;

    /** 是否全部满足分配 */
    @Schema(description = "是否全部满足分配")
    private Boolean fullyAllocated;

    /** 出库单当前状态 */
    @Schema(description = "出库单状态")
    private Integer status;

    /** 结果说明提示信息 */
    @Schema(description = "提示信息")
    private String message;

    /** 明细行分配结果详情 */
    @Schema(description = "明细行分配结果清单")
    private List<WmsLineAllocateResult> lineResults;

    @Data
    @Schema(description = "单行分配结果")
    public static class WmsLineAllocateResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long detailId;
        private Long skuId;
        private String skuCode;
        private String skuName;
        private Integer planCount;
        private Integer allocatedCount;
        private Integer shortageCount;
        private String batchNumber;
        private Integer locaId;
        private String locaCode;
        private Boolean isShortage;
    }
}
