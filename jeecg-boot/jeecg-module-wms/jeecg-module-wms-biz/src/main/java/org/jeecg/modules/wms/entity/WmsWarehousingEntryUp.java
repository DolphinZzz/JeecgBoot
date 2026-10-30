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
 * @Description: 入库上架记录（一行 = 一个明细拆到某个库位的一次上架）
 *
 * 推荐库位与实际库位不一致时仍允许上架，但要落 is_deviate / deviate_reason，
 * 偏离率是衡量推荐质量的核心指标（方案 3.5.3）。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@TableName("wms_warehousing_entry_up")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "入库上架记录")
public class WmsWarehousingEntryUp implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**入库明细ID*/
    @Schema(description = "入库明细ID")
    private Long entryDetailsId;

    /**入库单ID*/
    @Schema(description = "入库单ID")
    private Long warehousingEntryId;

    /**规格ID*/
    @Excel(name = "入库商品信息", width = 18, dictTable = "wms_item_sku_v", dicText = "text", dicCode = "id")
    @Dict(dictTable = "wms_item_sku_v", dicText = "text", dicCode = "id")
    @Schema(description = "规格ID")
    private Long skuId;

    /**批次号*/
    @Excel(name = "批次号", width = 16)
    @Schema(description = "批次号")
    private String batchNumber;

    /**上架仓库ID*/
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "上架仓库ID")
    private Integer warehouseId;

    /**上架库区ID*/
    @Dict(dictTable = "wms_warehouse_area", dicText = "name", dicCode = "id")
    @Schema(description = "上架库区ID")
    private Integer warehouseAreaId;

    /**上架库位ID*/
    @Excel(name = "上架库位", width = 16, dictTable = "wms_warehouse_area_loca", dicText = "loca_code", dicCode = "id")
    @Dict(dictTable = "wms_warehouse_area_loca", dicText = "loca_code", dicCode = "id")
    @Schema(description = "上架库位ID")
    private Integer warehouseAreaLocaId;

    /**上架数量*/
    @Excel(name = "上架数量", width = 12)
    @Schema(description = "上架数量")
    private Integer count;

    /**质量状态(1良品 2待检 3不良品)*/
    @Dict(dicCode = "wms_quality_status")
    @Schema(description = "质量状态(1良品 2待检 3不良品)")
    private Integer qualityStatus;

    /**本次使用的上架策略(FIXED/ABC/NEAREST)*/
    @Excel(name = "上架策略", width = 12, dicCode = "wms_putaway_strategy")
    @Dict(dicCode = "wms_putaway_strategy")
    @Schema(description = "本次使用的上架策略(FIXED/ABC/NEAREST)")
    private String strategyUsed;

    /**系统推荐库位ID*/
    @Dict(dictTable = "wms_warehouse_area_loca", dicText = "loca_code", dicCode = "id")
    @Schema(description = "系统推荐库位ID")
    private Integer recommendLocaId;

    /**是否偏离推荐(1是 0否)*/
    @Schema(description = "是否偏离推荐(1是 0否)")
    private Integer isDeviate;

    /**偏离原因*/
    @Schema(description = "偏离原因")
    private String deviateReason;

    /**是否固定库位溢出(1是 0否)*/
    @Schema(description = "是否固定库位溢出(1是 0否)")
    private Integer isOverflow;

    /**分拣明细ID*/
    @Schema(description = "分拣明细ID")
    private Long detailsSortId;

    /**上架状态(1未上架 2已上架)*/
    @Schema(description = "上架状态(1未上架 2已上架)")
    private Integer status;

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
