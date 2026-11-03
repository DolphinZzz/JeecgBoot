package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @Description: 库存管理 - Tab 2 全周期库存台账查询条件
 * @Author: jeecg-boot
 * @Date: 2026-11-01
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "库存管理-全周期库存台账查询条件")
public class WmsInventoryLedgerQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "所属仓库ID")
    private Integer warehouseId;

    @Schema(description = "商品编码")
    private String spuCode;

    @Schema(description = "商品名称")
    private String spuName;

    @Schema(description = "规格编码")
    private String skuCode;

    @Schema(description = "综合关键字(商品编码/名称/规格编码)")
    private String keyword;
}
