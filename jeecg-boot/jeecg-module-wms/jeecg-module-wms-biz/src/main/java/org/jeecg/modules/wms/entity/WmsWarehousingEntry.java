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
 * @Description: 入库单（ASN 头）
 *
 * 一张入库单对应一个批次（batch_number），明细行按 SKU 拆在
 * wms_warehousing_entry_details 上。状态机见 WmsEntryStatusEnum。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@TableName("wms_warehousing_entry")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "入库单")
public class WmsWarehousingEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    /**主键ID*/
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /**入库单号*/
    @Excel(name = "入库单号", width = 22)
    @Schema(description = "入库单号")
    private String entryCode;

    /**入库类型(1采购入库 2MES生产入库 3销退入库 4调拨入库)*/
    @Excel(name = "入库类型", width = 15, dicCode = "wms_entry_type")
    @Dict(dicCode = "wms_entry_type")
    @Schema(description = "入库类型(1采购入库 2MES生产入库 3销退入库 4调拨入库)")
    private Integer entryType;

    /**上游ERP采购单号*/
    @Schema(description = "上游ERP采购单号")
    private String erpPoNo;

    /**MES生产工单号：工业制造仓的生产入库在质检环节强制校验*/
    @Schema(description = "MES生产工单号")
    private String mesWorkOrderNo;

    /**货主ID*/
    @Excel(name = "货主", width = 18, dictTable = "wms_consignor", dicText = "consignor_name", dicCode = "id")
    @Dict(dictTable = "wms_consignor", dicText = "consignor_name", dicCode = "id")
    @Schema(description = "货主ID")
    private Long consignorId;

    /**批次号*/
    @Excel(name = "批次号", width = 16)
    @Schema(description = "批次号")
    private String batchNumber;

    /**批次商品数量（计划入库总数）*/
    @Excel(name = "计划数量", width = 12)
    @Schema(description = "批次商品数量")
    private Integer totalCount;

    /**预测到达时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "预测到达时间")
    private Date predictedArrivalTime;

    /**仓库ID*/
    @Excel(name = "仓库", width = 15, dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Dict(dictTable = "wms_warehouse", dicText = "name", dicCode = "id")
    @Schema(description = "仓库ID")
    private Integer warehouseId;

    /**破损个数*/
    @Excel(name = "破损个数", width = 12)
    @Schema(description = "破损个数")
    private Integer damageCount;

    /**超出数量（超收件数）*/
    @Excel(name = "超出数量", width = 12)
    @Schema(description = "超出数量")
    private Integer surpassCount;

    /**超收放行状态(0无需放行 1待主管确认 2已放行 3已驳回，决策 D4)*/
    @Dict(dicCode = "wms_over_receive_status")
    @Schema(description = "超收放行状态(0无需放行 1待主管确认 2已放行 3已驳回)")
    private Integer overReceiveAuditStatus;

    /**入库单状态(10待到货 20收货清点中 30待质检 40质检完成待上架 50上架中 60已完成 90异常挂起 99已取消)*/
    @Excel(name = "状态", width = 15, dicCode = "wms_entry_status")
    @Dict(dicCode = "wms_entry_status")
    @Schema(description = "入库单状态")
    private Integer status;

    /**缺少数量（少收件数，IN-08 自动登记待补料）*/
    @Excel(name = "缺少数量", width = 12)
    @Schema(description = "缺少数量")
    private Integer lackCount;

    /**少收待补料标识(0否 1是)*/
    @Schema(description = "少收待补料标识(0否 1是)")
    private Integer waitSupplementFlag;

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
