package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @Description: 整库区迁移明细行VO
 * @Author: jeecg-boot
 * @Date: 2026-11-02
 */
@Data
@Accessors(chain = true)
@Schema(description = "整库区迁移明细行")
public class WmsAreaMigrationDetailVO {

    @Schema(description = "源库位ID")
    private Integer sourceWarehouseAreaLocaId;

    @Schema(description = "源库位编码")
    private String sourceLocaCode;

    @Schema(description = "商品SPU名称")
    private String spuTitle;

    @Schema(description = "商品SKU ID")
    private Long skuId;

    @Schema(description = "商品SKU编码")
    private String skuCode;

    @Schema(description = "批次号")
    private String batchNumber;

    @Schema(description = "货主ID")
    private Long consignorId;

    @Schema(description = "质量状态(1良品 2待检 3不良品)")
    private Integer qualityStatus;

    @Schema(description = "迁移件数")
    private Integer count;

    @Schema(description = "自动匹配的目标库位ID")
    private Integer targetWarehouseAreaLocaId;

    @Schema(description = "自动匹配的目标库位编码")
    private String targetLocaCode;

    @Schema(description = "匹配状态(MATCHED已配对 MISSING缺目标库位)")
    private String matchStatus;

    @Schema(description = "目标库区内可供选择的空库位候选列表")
    private List<WmsLocaOptionVO> candidateTargetLocas;
}
