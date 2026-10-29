package org.jeecg.modules.wms.entity;

import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 库存变动流水
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@TableName("wms_inventory_log")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "库存变动流水")
public class WmsInventoryLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**库存行ID*/
    @Schema(description = "库存行ID")
    private Long inventoryId;

    /**业务类型(1入库上架 2出库扣减 3移库移出 4移库移入 5补货 6盘点调账 7冻结 8解冻 9锁定 10解锁)*/
    @Dict(dicCode = "wms_inventory_biz_type")
    @Schema(description = "业务类型")
    private Integer bizType;

    /**关联单号*/
    @Schema(description = "关联单号")
    private String bizNo;

    /**仓库ID*/
    @Schema(description = "仓库ID")
    private Integer warehouseId;

    /**库区ID*/
    @Schema(description = "库区ID")
    private Integer warehouseAreaId;

    /**库位ID*/
    @Schema(description = "库位ID")
    private Integer warehouseAreaLocaId;

    /**货主ID*/
    @Schema(description = "货主ID")
    private Long consignorId;

    /**规格ID*/
    @Schema(description = "规格ID")
    private Long skuId;

    /**批次号*/
    @Schema(description = "批次号")
    private String batchNumber;

    /**变动数量(正增负减，数量不变的操作记0)*/
    @Schema(description = "变动数量")
    private Integer changeCount;

    /**变动前数量*/
    @Schema(description = "变动前数量")
    private Integer beforeCount;

    /**变动后数量*/
    @Schema(description = "变动后数量")
    private Integer afterCount;

    /**备注*/
    @Schema(description = "备注")
    private String remark;

    /**SaaS标识*/
    @Schema(description = "SaaS标识", hidden = true)
    private String saasId;

    /**创建人*/
    @Schema(description = "创建人")
    private String createBy;

    /**创建时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;
}
