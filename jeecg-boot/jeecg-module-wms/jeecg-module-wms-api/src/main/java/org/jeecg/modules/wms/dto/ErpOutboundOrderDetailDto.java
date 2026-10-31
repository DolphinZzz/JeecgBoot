package org.jeecg.modules.wms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description: ERP/MES 推送出库单明细 DTO
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "ERP推送出库单明细DTO")
public class ErpOutboundOrderDetailDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品SKU ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long skuId;

    @Schema(description = "出库数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer outCount;

    @Schema(description = "指定批次号(选填，MES工单领料场景强匹配)")
    private String batchNumber;

    @Schema(description = "缺货处理(1剔除缺货行 2整单挂起)，默认1")
    private Integer shortageHandle;
}
