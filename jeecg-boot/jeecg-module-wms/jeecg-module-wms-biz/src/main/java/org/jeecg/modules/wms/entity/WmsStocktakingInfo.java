package org.jeecg.modules.wms.entity;

import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 盘点记录（一行 = 某库存维度的账面数 vs 实盘数）
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@TableName("wms_stocktaking_info")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "盘点记录")
public class WmsStocktakingInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**盘点批次号*/
    @Excel(name = "盘点批次号", width = 20)
    @Schema(description = "盘点批次号")
    private String batchNo;

    /**盘点仓库ID*/
    @Excel(name = "盘点仓库", width = 15, dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "盘点仓库ID")
    private Integer targetWarehouseId;

    /**盘点库区ID*/
    @Dict(dictTable = "wms_warehouse_area", dicText = "name", dicCode = "id")
    @Schema(description = "盘点库区ID")
    private Integer targetWarehouseAreaId;

    /**盘点库位ID*/
    @Excel(name = "盘点库位", width = 15, dictTable = "wms_warehouse_area_loca", dicText = "loca_code", dicCode = "id")
    @Dict(dictTable = "wms_warehouse_area_loca", dicText = "loca_code", dicCode = "id")
    @Schema(description = "盘点库位ID")
    private Integer targetWarehouseAreaLocaId;

    /**规格ID*/
    @Excel(name = "商品SKU", width = 15, dictTable = "wms_item_sku", dicText = "code", dicCode = "id")
    @Dict(dictTable = "wms_item_sku", dicText = "code", dicCode = "id")
    @Schema(description = "规格ID")
    private Long skuId;

    /**货主ID*/
    @Dict(dictTable = "wms_customer", dicText = "customer_name", dicCode = "id")
    @Schema(description = "货主ID")
    private Long consignorId;

    /**批次号*/
    @Schema(description = "批次号")
    private String batchNumber;

    /**有效期至*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "有效期至")
    private Date expireDate;

    /**质量状态(1良品 2待检 3不良品)*/
    @Dict(dicCode = "wms_quality_status")
    @Schema(description = "质量状态(1良品 2待检 3不良品)")
    private Integer qualityStatus;

    /**账面数量*/
    @Excel(name = "账面数量", width = 12)
    @Schema(description = "账面数量")
    private Integer bookCount;

    /**盘点数量（实盘，未录入时为空）*/
    @Excel(name = "盘点数量", width = 12)
    @Schema(description = "盘点数量")
    private Integer correctCount;

    /**差异数量 = 实盘 - 账面*/
    @Excel(name = "差异数量", width = 12)
    @Schema(description = "差异数量")
    private Integer discrepancyCount;

    /**差异审批状态(0无差异 1待审批 2已通过 3已驳回)*/
    @Excel(name = "差异审批", width = 12, dicCode = "wms_stocktaking_audit_status")
    @Dict(dicCode = "wms_stocktaking_audit_status")
    @Schema(description = "差异审批状态(0无差异 1待审批 2已通过 3已驳回)")
    private Integer auditStatus;

    /**审批人*/
    @Schema(description = "审批人")
    private String auditBy;

    /**审批时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "审批时间")
    private Date auditTime;

    /**类型(1滚动盘点 2定时盘点)*/
    @Excel(name = "盘点类型", width = 12, dicCode = "wms_stocktaking_category")
    @Dict(dicCode = "wms_stocktaking_category")
    @Schema(description = "类型(1滚动盘点 2定时盘点)")
    private Integer category;

    /**状态(1待盘点 2已盘待审核 3已确认/已调账)*/
    @Excel(name = "状态", width = 12, dicCode = "wms_stocktaking_status")
    @Dict(dicCode = "wms_stocktaking_status")
    @Schema(description = "状态(1待盘点 2已盘待审核 3已确认/已调账)")
    private Integer status;

    /**操作员工ID*/
    @Schema(description = "操作员工ID")
    private Integer empId;

    /**定时任务ID*/
    @Schema(description = "定时任务ID")
    private Long stocktakingTaskId;

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

    /**修改人*/
    @Schema(description = "修改人")
    private String updateBy;

    /**修改时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    private Date updateTime;

    /**是否删除 1未删除 2已删除*/
    @TableLogic(value = "1", delval = "2")
    @Schema(description = "是否删除 1未删除 2已删除")
    private Integer isDel;
}
