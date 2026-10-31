package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 确认拣货入参表单
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "确认拣货表单")
public class WmsPickConfirmForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 出库单ID */
    @NotNull(message = "出库单ID不能为空")
    @Schema(description = "出库单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long outboundOrderId;

    /** 拣货明细行 */
    @NotEmpty(message = "拣货明细不能为空")
    @Schema(description = "拣货明细行", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<WmsPickLineItem> pickLines;

    @Data
    @Schema(description = "拣货行")
    public static class WmsPickLineItem implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 明细ID */
        @NotNull(message = "明细ID不能为空")
        @Schema(description = "明细ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long detailId;

        /** 实拣数量 */
        @NotNull(message = "实拣数量不能为空")
        @Schema(description = "实拣数量", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer pickedCount;

        /** 扫描的SN序列号集合（若SKU启用了SN管理，必须扫入对应数量的SN） */
        @Schema(description = "扫描的SN序列号集合")
        private List<String> snCodes;
    }
}
