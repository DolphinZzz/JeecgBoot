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
 * @Description: 质检单（IQC，一行 = 一个入库明细的一次质检）
 *
 * 免检的明细在收货时会自动生成一条 check_count = 送检数、pass_count = 送检数
 * 的免检放行记录，保证「收货 → 质检 → 上架」的顺序留痕不缺口（IN-01）。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@TableName("wms_quality_check")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "质检单")
public class WmsQualityCheck implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**质检单号*/
    @Excel(name = "质检单号", width = 22)
    @Schema(description = "质检单号")
    private String qcCode;

    /**入库单ID*/
    @Schema(description = "入库单ID")
    private Long warehousingEntryId;

    /**入库明细ID*/
    @Schema(description = "入库明细ID")
    private Long entryDetailsId;

    /**MES生产工单号*/
    @Schema(description = "MES生产工单号")
    private String mesWorkOrderNo;

    /**规格ID*/
    @Excel(name = "商品SKU", width = 18, dictTable = "wms_item_sku", dicText = "code", dicCode = "id")
    @Dict(dictTable = "wms_item_sku", dicText = "code", dicCode = "id")
    @Schema(description = "规格ID")
    private Long skuId;

    /**批次号*/
    @Excel(name = "批次号", width = 16)
    @Schema(description = "批次号")
    private String batchNumber;

    /**质检方式(1免检 2抽检 3全检)*/
    @Excel(name = "质检方式", width = 12, dicCode = "wms_qc_type")
    @Dict(dicCode = "wms_qc_type")
    @Schema(description = "质检方式(1免检 2抽检 3全检)")
    private Integer qcType;

    /**送检数量*/
    @Excel(name = "送检数量", width = 12)
    @Schema(description = "送检数量")
    private Integer checkCount;

    /**抽检数量*/
    @Excel(name = "抽检数量", width = 12)
    @Schema(description = "抽检数量")
    private Integer sampleCount;

    /**合格数量*/
    @Excel(name = "合格数量", width = 12)
    @Schema(description = "合格数量")
    private Integer passCount;

    /**不合格数量*/
    @Excel(name = "不合格数量", width = 12)
    @Schema(description = "不合格数量")
    private Integer failCount;

    /**不合格原因*/
    @Schema(description = "不合格原因")
    private String failReason;

    /**关联退货单号（不合格品分流退货区时回填）*/
    @Schema(description = "关联退货单号")
    private String returnOrderNo;

    /**质检员工ID*/
    @Schema(description = "质检员工ID")
    private Integer qcEmpId;

    /**质检时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "质检时间")
    private Date qcTime;

    /**状态(1待质检 2质检中 3合格 4不合格 5部分合格)*/
    @Excel(name = "状态", width = 12, dicCode = "wms_qc_status")
    @Dict(dicCode = "wms_qc_status")
    @Schema(description = "状态(1待质检 2质检中 3合格 4不合格 5部分合格)")
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
