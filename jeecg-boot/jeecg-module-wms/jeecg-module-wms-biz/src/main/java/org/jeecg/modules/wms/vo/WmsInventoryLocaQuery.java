package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @Description: 库存管理 - Tab 1 库位列表查询条件
 * @Author: jeecg-boot
 * @Date: 2026-11-01
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "库存管理-库位列表查询条件")
public class WmsInventoryLocaQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "仓库ID")
    private Integer warehouseId;

    @Schema(description = "库区ID")
    private Integer warehouseAreaId;

    @Schema(description = "库位编号(支持模糊查询)")
    private String locaCode;

    @Schema(description = "库位状态(1空闲 2占用 3盘点锁定 4禁用)")
    private Integer locaStatus;

    @Schema(description = "是否占用(0空闲 1已占用)")
    private Integer isOccupied;

    @Schema(description = "商品SKU ID")
    private Long skuId;

    @Schema(description = "商品编码/规格编码/商品名称(模糊匹配)")
    private String skuKeyword;

    @Schema(description = "是否混合库位(0单品库 1混合库)")
    private Integer isMixed;

    @Schema(description = "规划存放的商品SPU ID")
    private Long spuId;

    @Schema(description = "商品编码/名称(模糊匹配)")
    private String spuKeyword;
}
