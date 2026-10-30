package org.jeecg.modules.wms.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description: 商品选择器列表展示 VO
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "商品选择器数据")
public class WmsItemSkuSelectVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "SKU规格ID")
    private Long id;

    @Schema(description = "SPU商品ID")
    private Long spuId;

    @Schema(description = "SKU条码/规格编码")
    private String skuCode;

    @Schema(description = "商品编码")
    private String spuCode;

    @Schema(description = "商品名称")
    private String spuName;

    @Schema(description = "商品类别名称")
    private String categoryName;

    @Schema(description = "商品类别ID")
    private Integer categoryId;

    @Schema(description = "单价(元)")
    private BigDecimal price;

    @Schema(description = "默认供应商ID")
    private Long supplierId;

    @Schema(description = "默认供应商名称")
    private String supplierName;

    @Schema(description = "是否SN管理(1是 0否)")
    private Integer isSnManaged;
}
