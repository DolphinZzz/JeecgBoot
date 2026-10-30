package org.jeecg.modules.wms.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: ERP 推送入库单明细 DTO
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "ERP推送入库单明细")
public class ErpWarehousingEntryDetailDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "入库商品规格ID/SKU ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long skuId;

    @Schema(description = "计划入库数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer count;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "采购单价(元)")
    private BigDecimal price;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生产日期")
    private Date prodDate;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "有效期至")
    private Date expireDate;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "预计到达时间")
    private Date predictedArrivalTime;
}
