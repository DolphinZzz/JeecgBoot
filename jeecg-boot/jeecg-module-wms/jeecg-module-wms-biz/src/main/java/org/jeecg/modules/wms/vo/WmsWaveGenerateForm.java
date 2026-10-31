package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 自动分波生成表单
 *
 * 决策 D3：按「承运商 + 温区」维度分波
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "自动分波表单")
public class WmsWaveGenerateForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 目标仓库ID */
    @NotNull(message = "仓库ID不能为空")
    @Schema(description = "目标仓库ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer warehouseId;

    /** 指定参与分波的出库单ID集合（为空则自动汇总该仓库下所有已锁库、波次播种法且未分波的订单） */
    @Schema(description = "指定出库单ID集合")
    private List<Long> outboundOrderIds;
}
