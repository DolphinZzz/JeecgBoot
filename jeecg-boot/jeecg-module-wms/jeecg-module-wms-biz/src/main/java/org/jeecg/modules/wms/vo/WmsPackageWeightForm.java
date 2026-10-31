package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 打包称重表单
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "打包称重表单")
public class WmsPackageWeightForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 出库单ID */
    @NotNull(message = "出库单ID不能为空")
    @Schema(description = "出库单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long outboundOrderId;

    /** 实测总重量（克） */
    @NotNull(message = "实测总重量不能为空")
    @Schema(description = "实测总重量(克)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer actualWeight;

    /** 物流公司/承运商ID */
    @Schema(description = "承运商ID")
    private Long logisticsCompanyId;

    /** 物流面单号（可选，打包时录入或接口获取） */
    @Schema(description = "物流单号")
    private String logisticsNumber;

    /** 打包明细箱单清单 */
    @Schema(description = "打包箱单清单")
    private List<WmsPackageBoxItem> packages;

    @Data
    @Schema(description = "打包箱单行")
    public static class WmsPackageBoxItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long detailId;
        private Long skuId;
        private String packageCode;
        private Integer packageCount;
    }
}
