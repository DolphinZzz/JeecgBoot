package org.jeecg.modules.wms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: ERP/MES 推送出库单 DTO
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "ERP推送出库单请求参数")
public class ErpOutboundOrderCreateDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "上游ERP销售/调拨单号")
    private String erpOrderNo;

    @Schema(description = "出库类型(1销售出库 2MES工单领料 3采购退货 4调拨出库)，默认1销售出库")
    private Integer outboundType;

    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer warehouseId;

    @Schema(description = "货主ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long consignorId;

    @Schema(description = "客户ID(销售出库必填)")
    private Long customerId;

    @Schema(description = "MES生产工单号(工业仓领料必填)")
    private String mesWorkOrderNo;

    @Schema(description = "承运商/物流公司ID")
    private Long logisticsCompanyId;

    @Schema(description = "出库明细行列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ErpOutboundOrderDetailDto> details;
}
