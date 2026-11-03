package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 出库单新建/提交表单
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "新建出库单表单")
public class WmsOutboundCreateForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 出库类型(1销售出库 2MES工单领料 3采购退货 4调拨出库) */
    @NotNull(message = "出库类型不能为空")
    @Schema(description = "出库类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer outboundType;

    /** 仓库ID */
    @NotNull(message = "出库仓库不能为空")
    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer warehouseId;

    /** 货主ID（可选，由明细所选库存自动带出） */
    @Schema(description = "货主ID（可选，由明细所选库存自动带出）")
    private Long consignorId;

    /** 客户ID（销售出库必填） */
    @Schema(description = "客户ID")
    private Long customerId;

    /** 上游ERP单号 */
    @Schema(description = "上游ERP单号")
    private String erpOrderNo;

    /** MES生产工单号（工业仓领料必填） */
    @Schema(description = "MES生产工单号")
    private String mesWorkOrderNo;

    /** 承运商/物流公司ID */
    @Schema(description = "承运商/物流公司ID")
    private Long logisticsCompanyId;

    /** 出库明细清单 */
    @NotEmpty(message = "出库明细不能为空")
    @Valid
    @Schema(description = "出库明细清单", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<WmsOutboundDetailForm> details;
}
