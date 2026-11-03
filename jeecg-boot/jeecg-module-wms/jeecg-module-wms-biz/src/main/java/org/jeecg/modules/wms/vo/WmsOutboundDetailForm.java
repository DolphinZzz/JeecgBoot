package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @Description: 出库单明细提交表单
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "出库单明细表单")
public class WmsOutboundDetailForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 规格ID */
    @NotNull(message = "商品SKU不能为空")
    @Schema(description = "规格ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long skuId;

    /** 出库库位ID */
    @NotNull(message = "出库库位不能为空")
    @Schema(description = "出库库位ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer warehouseAreaLocaId;

    /** 库区ID */
    @Schema(description = "出库库区ID")
    private Integer warehouseAreaId;

    /** 计划出库数量 */
    @NotNull(message = "计划出库数量不能为空")
    @Min(value = 1, message = "计划出库数量必须大于 0")
    @Schema(description = "计划出库数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer outCount;

    /** 指定批次号（可选，MES工单领料场景强匹配） */
    @Schema(description = "指定批次号")
    private String batchNumber;

    /** 缺货处理(1剔除缺货行 2整单挂起) */
    @Schema(description = "缺货处理(1剔除缺货行 2整单挂起)")
    private Integer shortageHandle;
}
