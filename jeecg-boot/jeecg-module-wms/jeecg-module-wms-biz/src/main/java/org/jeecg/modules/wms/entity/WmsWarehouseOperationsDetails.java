package org.jeecg.modules.wms.entity;

import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 库内作业明细
 * 一行 = 一次「某库位某批次移到另一库位」或「补货到拣货区」，移库的执行以行为单位原子完成。
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@TableName("wms_warehouse_operations_details")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "库内作业明细")
public class WmsWarehouseOperationsDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**作业单ID*/
    @Schema(description = "作业单ID")
    private Long warehouseOperationsId;

    /**规格ID*/
    @Dict(dictTable = "wms_item_sku", dicText = "code", dicCode = "id")
    @Schema(description = "规格ID")
    private Long skuId;

    /**批次号*/
    @Schema(description = "批次号")
    private String batchNumber;

    /**货主ID*/
    @Dict(dictTable = "wms_customer", dicText = "customer_name", dicCode = "id")
    @Schema(description = "货主ID")
    private Long consignorId;

    /**质量状态(1良品 2待检 3不良品)，移库时源/目标库存行保持一致*/
    @Dict(dicCode = "wms_quality_status")
    @Schema(description = "质量状态(1良品 2待检 3不良品)")
    private Integer qualityStatus;

    /**移出数量 / 补货数量（与该行 category=1 记录一致）*/
    @Schema(description = "移出数量/补货数量")
    private Integer sourceCount;

    /**移入数量（与该行 category=2 记录一致）*/
    @Schema(description = "移入数量")
    private Integer targetCount;

    /**来源库区ID*/
    @Dict(dictTable = "wms_warehouse_area", dicText = "name", dicCode = "id")
    @Schema(description = "来源库区ID")
    private Integer sourceWarehouseAreaId;

    /**来源库位ID*/
    @Dict(dictTable = "wms_warehouse_area_loca", dicText = "loca_code", dicCode = "id")
    @Schema(description = "来源库位ID")
    private Integer sourceWarehouseAreaLocaId;

    /**目标仓库ID*/
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "目标仓库ID")
    private Integer targetWarehouseId;

    /**目标库区ID*/
    @Dict(dictTable = "wms_warehouse_area", dicText = "name", dicCode = "id")
    @Schema(description = "目标库区ID")
    private Integer targetWarehouseAreaId;

    /**目标库位ID*/
    @Dict(dictTable = "wms_warehouse_area_loca", dicText = "loca_code", dicCode = "id")
    @Schema(description = "目标库位ID")
    private Integer targetWarehouseAreaLocaId;

    /**商品价格（沿用既有列，二期不使用）*/
    @Schema(description = "商品价格", hidden = true)
    private Integer price;

    /**类型(1来源 2目标)*/
    @Schema(description = "类型(1来源 2目标)", hidden = true)
    private Integer category;
}
