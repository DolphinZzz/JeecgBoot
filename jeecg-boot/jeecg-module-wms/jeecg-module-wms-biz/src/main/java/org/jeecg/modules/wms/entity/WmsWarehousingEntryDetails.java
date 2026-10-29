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
 * @Description: 入库单明细（一行 = 一个 SKU 的计划数 / 实收数 / 质检结论）
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@TableName("wms_warehousing_entry_details")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "入库单明细")
public class WmsWarehousingEntryDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**规格ID*/
    @Excel(name = "商品SKU", width = 18, dictTable = "wms_item_sku", dicText = "code", dicCode = "id")
    @Dict(dictTable = "wms_item_sku", dicText = "code", dicCode = "id")
    @Schema(description = "规格ID")
    private Long skuId;

    /**批次号*/
    @Excel(name = "批次号", width = 16)
    @Schema(description = "批次号")
    private String batchNumber;

    /**生产日期*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生产日期")
    private Date prodDate;

    /**有效期至*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "有效期至")
    private Date expireDate;

    /**计划数量*/
    @Excel(name = "计划数量", width = 12)
    @Schema(description = "数量")
    private Integer count;

    /**实收数量*/
    @Excel(name = "实收数量", width = 12)
    @Schema(description = "实收数量")
    private Integer receivedCount;

    /**供应商ID*/
    @Excel(name = "供应商", width = 18, dictTable = "wms_supplier", dicText = "supplier_name", dicCode = "id")
    @Dict(dictTable = "wms_supplier", dicText = "supplier_name", dicCode = "id")
    @Schema(description = "供应商ID")
    private Long supplierId;

    /**单价（分）*/
    @Excel(name = "单价", width = 12)
    @Schema(description = "单价")
    private Integer price;

    /**入库单ID*/
    @Schema(description = "入库单ID")
    private Long warehousingEntryId;

    /**实际到达时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "实际到达时间")
    private Date realArrivalTime;

    /**预测到达时间*/
    @Schema(description = "预测到达时间")
    private Date predictedArrivalTime;

    /**卸货员工ID*/
    @Schema(description = "卸货员工ID")
    private Integer unloadEmpId;

    /**卸货时间*/
    @Schema(description = "卸货时间")
    private Date unloadTime;

    /**状态(1未到货 2已到货 3已卸货 4分拣中 5完成分拣)*/
    @Schema(description = "状态(1未到货 2已到货 3已卸货 4分拣中 5完成分拣)")
    private Integer status;

    /**质检状态(1待质检 2质检中 3合格 4不合格 5部分合格)*/
    @Excel(name = "质检状态", width = 12, dicCode = "wms_qc_status")
    @Dict(dicCode = "wms_qc_status")
    @Schema(description = "质检状态(1待质检 2质检中 3合格 4不合格 5部分合格)")
    private Integer qcStatus;

    /**质检合格数*/
    @Excel(name = "质检合格数", width = 12)
    @Schema(description = "质检合格数")
    private Integer qcPassCount;

    /**质检不合格数*/
    @Excel(name = "质检不合格数", width = 12)
    @Schema(description = "质检不合格数")
    private Integer qcFailCount;

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
