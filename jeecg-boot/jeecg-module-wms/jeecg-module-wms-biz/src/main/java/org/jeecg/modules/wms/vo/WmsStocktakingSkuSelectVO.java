package org.jeecg.modules.wms.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description: 盘库单商品选择器 VO
 * @Author: jeecg-boot
 * @Date: 2026-11-03
 * @Version: V1.0
 */
@Data
@Schema(description = "盘库商品选择数据")
public class WmsStocktakingSkuSelectVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "SKU规格ID")
    private Long id;

    @Schema(description = "SPU商品ID")
    private Long spuId;

    @Schema(description = "规格编号")
    private String skuCode;

    @Schema(description = "规格名")
    private String skuName;

    @Schema(description = "商品编号")
    private String spuCode;

    @Schema(description = "商品名称")
    private String spuName;

    @Schema(description = "商品分类名称")
    private String categoryName;

    @Schema(description = "商品分类ID")
    private Integer categoryId;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "入库价/成本价(元)")
    private BigDecimal inPrice;

    @Schema(description = "销售单价(元)")
    private BigDecimal price;

    @Schema(description = "当前仓库账面库存(若传入warehouseId)")
    private Integer bookCount;
}
