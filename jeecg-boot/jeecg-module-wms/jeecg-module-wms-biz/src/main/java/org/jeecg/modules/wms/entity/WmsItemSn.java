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
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: SN序列号追踪
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@TableName("wms_item_sn")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "SN序列号追踪")
public class WmsItemSn implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**SN序列号*/
    @Schema(description = "SN序列号")
    private String snCode;

    /**规格ID*/
    @Dict(dictTable = "wms_item_sku", dicText = "code", dicCode = "id")
    @Schema(description = "规格ID")
    private Long skuId;

    /**批次号*/
    @Schema(description = "批次号")
    private String batchNumber;

    /**所在仓库ID*/
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "所在仓库ID")
    private Integer warehouseId;

    /**所在库位ID*/
    @Dict(dictTable = "wms_warehouse_area_loca", dicText = "loca_code", dicCode = "id")
    @Schema(description = "所在库位ID")
    private Integer warehouseAreaLocaId;

    /**质量状态(1良品 2待检 3不良品)*/
    @Dict(dicCode = "wms_quality_status")
    @Schema(description = "质量状态(1良品 2待检 3不良品)")
    private Integer qualityStatus;

    /**入库单ID*/
    @Schema(description = "入库单ID")
    private Long entryOrderId;

    /**出库单ID*/
    @Schema(description = "出库单ID")
    private Long outboundOrderId;

    /**MES生产工单号*/
    @Schema(description = "MES生产工单号")
    private String mesWorkOrderNo;

    /**状态(1收货待检 2在库良品 3在库不良 4拣货锁定 5已出库 6已退货)*/
    @Dict(dicCode = "wms_sn_status")
    @Schema(description = "状态(1收货待检 2在库良品 3在库不良 4拣货锁定 5已出库 6已退货)")
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
