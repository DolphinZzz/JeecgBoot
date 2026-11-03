package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 库存管理 - Tab 1 库位列表视图对象
 * @Author: jeecg-boot
 * @Date: 2026-11-01
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "库存管理-库位列表视图对象")
public class WmsInventoryLocaPageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "库位ID")
    private Integer locaId;

    @Schema(description = "库位编码")
    private String locaCode;

    @Schema(description = "所属仓库ID")
    private Integer warehouseId;

    @Schema(description = "所属仓库名称")
    private String warehouseName;

    @Schema(description = "所属库区ID")
    private Integer warehouseAreaId;

    @Schema(description = "所属库区名称")
    private String areaName;

    @Schema(description = "库区类别")
    private String areaCategory;

    @Schema(description = "巷道号")
    private String tunnelNumber;

    @Schema(description = "货架号")
    private String shelfNumber;

    @Schema(description = "层号")
    private String layerNumber;

    @Schema(description = "位号")
    private String positionNumber;

    @Schema(description = "物理坐标文本")
    private String positionText;

    @Schema(description = "库位状态(1空闲 2占用 3盘点锁定 4禁用 5满仓)")
    private Integer locaStatus;

    @Schema(description = "库位状态文本")
    private String locaStatusText;

    @Schema(description = "存储条件")
    private String storageCondition;

    @Schema(description = "固定库位绑定SKU ID")
    private Long fixedSkuId;

    @Schema(description = "绑定/在库SKU ID")
    private Long boundSkuId;

    @Schema(description = "商品名称")
    private String spuName;

    @Schema(description = "规格编码")
    private String skuCode;

    @Schema(description = "规格名称/规格值")
    private String skuName;

    @Schema(description = "当前在库批次号")
    private String batchNumber;

    @Schema(description = "在库总件数(实际物理库存)")
    private Integer totalCount;

    @Schema(description = "可用件数")
    private Integer availableCount;

    @Schema(description = "锁定件数")
    private Integer lockedCount;

    @Schema(description = "冻结件数")
    private Integer frozenCount;

    @Schema(description = "是否占用(0空闲 1已占用)")
    private Integer isOccupied;

    @Schema(description = "是否混合库位(0单品库 1混合库)")
    private Integer isMixed;

    @Schema(description = "已规划SPU数量")
    private Integer spuCount;

    @Schema(description = "允许存放的商品及上限摘要")
    private String spuNamesText;

    @Schema(description = "规划总容量(件数)")
    private Integer totalCapacity;

    @Schema(description = "当前在库总件数")
    private Integer currentCount;

    @Schema(description = "剩余可放件数")
    private Integer remainCount;

    @Schema(description = "库位利用率百分比")
    private Double usageRate;

    @Schema(description = "是否允许混SKU")
    private Integer allowMixSku;

    @Schema(description = "是否允许混批次")
    private Integer allowMixBatch;

    @Schema(description = "商品规划明细列表")
    private List<WmsLocaCapacityItemVO> spuCapacityList;
}
