package org.jeecg.modules.wms.vo;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 入库单明细入参
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@Schema(description = "入库单明细")
public class WmsEntryDetailForm {

    /**SKU ID*/
    @Schema(description = "SKU ID")
    private Long skuId;

    /**计划数量*/
    @Schema(description = "计划数量")
    private Integer count;

    /**供应商ID*/
    @Schema(description = "供应商ID")
    private Long supplierId;

    /**单价（元），落库按分存储*/
    @Schema(description = "单价(元)")
    private BigDecimal price;

    /**生产日期。前端明细表格的日期列只传 yyyy-MM-dd，必须显式声明，
     * 否则落到全局的 yyyy-MM-dd HH:mm:ss 反序列化格式上会解析失败*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生产日期")
    private Date prodDate;

    /**有效期至，留空按 SPU 保质期天数推算*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "有效期至")
    private Date expireDate;

    /**预测到达时间*/
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "预测到达时间")
    private Date predictedArrivalTime;
}
