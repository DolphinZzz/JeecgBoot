package org.jeecg.modules.wms.vo;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description: 确认上架入参（PDA 扫库位码后提交）
 *
 * 未指定 locaId 时用系统推荐库位；指定了但与推荐不一致，只要通过硬过滤就允许，
 * 并记录 is_deviate / deviateReason（方案 3.5.3 的人工偏离）。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@Schema(description = "确认上架")
public class WmsPutawayConfirmForm {

    /**入库单ID*/
    @Schema(description = "入库单ID")
    private Long id;

    /**上架行；为空表示按推荐方案整单上架*/
    @Schema(description = "上架行")
    private List<WmsPutawayLine> details;

    @Data
    @Schema(description = "单个上架落点")
    public static class WmsPutawayLine {

        /**入库明细ID*/
        @Schema(description = "入库明细ID")
        private Long detailId;

        /**上架数量*/
        @Schema(description = "上架数量")
        private Integer count;

        /**目标库位ID（为空用推荐值）*/
        @Schema(description = "目标库位ID")
        private Integer locaId;

        /**偏离原因（实际库位与推荐不一致时填写）*/
        @Schema(description = "偏离原因")
        private String deviateReason;

        /**
         * 强制指定库位：为 true 时跳过分区/温区/混放/容积承重校验，只守跨仓与库位不可用，
         * 且 deviateReason 必填。用于系统推荐为空（如本仓没有贵品冷库区）时的异常处置。
         */
        @Schema(description = "是否强制指定库位")
        private Boolean force;
    }
}
