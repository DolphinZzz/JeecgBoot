package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @Description: 库存管理 - Tab 2 全周期库存台账 VO
 * 涵盖：商品编码、商品名称、规格编码、库存数量、可用数量、锁定数量、冻结数量、
 * 待卸货数量、待分拣数量、已分拣数量、欠货数量。
 * @Author: jeecg-boot
 * @Date: 2026-11-01
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "库存管理-全周期库存台账VO")
public class WmsInventoryLedgerVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "SPU ID")
    private Long spuId;

    @Schema(description = "所属仓库ID")
    private Integer warehouseId;

    @Schema(description = "所属仓库名称")
    private String warehouseName;

    @Schema(description = "商品编码")
    private String spuCode;

    @Schema(description = "商品名称")
    private String spuName;

    @Schema(description = "规格编码")
    private String skuCode;

    @Schema(description = "规格条形码")
    private String barCode;

    @Schema(description = "库存数量(物理总库存)")
    private Integer totalCount;

    @Schema(description = "在库库位数")
    private Integer inStockLocaCount;

    @Schema(description = "可用数量")
    private Integer availableCount;

    @Schema(description = "锁定数量")
    private Integer lockedCount;

    @Schema(description = "冻结数量")
    private Integer frozenCount;

    @Schema(description = "待卸货数量")
    private Integer pendingUnloadCount;

    @Schema(description = "待分拣数量")
    private Integer pendingSortCount;

    @Schema(description = "已分拣数量")
    private Integer sortedCount;

    @Schema(description = "欠货数量")
    private Integer shortageCount;
}
