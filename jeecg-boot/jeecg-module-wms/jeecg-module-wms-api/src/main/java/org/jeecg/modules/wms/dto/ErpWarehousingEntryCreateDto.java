package org.jeecg.modules.wms.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: ERP 系统推送入库单 DTO
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "ERP推送入库单请求参数")
public class ErpWarehousingEntryCreateDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "上游ERP采购单号/单据编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String erpPoNo;

    @Schema(description = "入库类型(1采购入库 2MES生产入库 3销退入库 4调拨入库)，默认1采购入库")
    private Integer entryType;

    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer warehouseId;

    @Schema(description = "货主ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long consignorId;

    @Schema(description = "批次号，若不传系统自动生成整单唯一批次")
    private String batchNumber;

    @Schema(description = "MES生产工单号(选填)")
    private String mesWorkOrderNo;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "预计到达时间")
    private Date predictedArrivalTime;

    @Schema(description = "入库明细行列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ErpWarehousingEntryDetailDto> details;
}
