package org.jeecg.modules.wms.vo;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 新建入库单入参（ASN）
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@Schema(description = "新建入库单")
public class WmsEntryCreateForm {

    /**入库类型(1采购入库 2MES生产入库 3销退入库 4调拨入库)*/
    @Schema(description = "入库类型(1采购入库 2MES生产入库 3销退入库 4调拨入库)")
    private Integer entryType;

    /**仓库ID*/
    @Schema(description = "仓库ID")
    private Integer warehouseId;

    /**货主ID*/
    @Schema(description = "货主ID")
    private Long consignorId;

    /**批次号，留空自动生成*/
    @Schema(description = "批次号，留空自动生成")
    private String batchNumber;

    /**上游ERP采购单号*/
    @Schema(description = "上游ERP采购单号")
    private String erpPoNo;

    /**MES生产工单号（生产入库时填写，工业仓质检强制校验）*/
    @Schema(description = "MES生产工单号")
    private String mesWorkOrderNo;

    /**预测到达时间，留空取当前时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "预测到达时间")
    private Date predictedArrivalTime;

    /**明细行*/
    @Schema(description = "明细行")
    private List<WmsEntryDetailForm> details;
}
