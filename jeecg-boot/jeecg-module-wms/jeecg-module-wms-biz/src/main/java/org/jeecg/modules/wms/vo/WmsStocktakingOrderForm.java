package org.jeecg.modules.wms.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.modules.wms.entity.WmsStocktakingDetail;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * @Description: 盘库单提交表单（主从聚合）
 * @Author: jeecg-boot
 * @Date: 2026-11-03
 * @Version: V1.0
 */
@Data
@Schema(description = "盘库单提交表单")
public class WmsStocktakingOrderForm implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID（编辑/暂存更新时必传）")
    private Long id;

    @Schema(description = "盘库单号")
    private String orderNo;

    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer warehouseId;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "单据日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date orderDate;

    @Schema(description = "实际总金额")
    private BigDecimal totalActualAmount;

    @Schema(description = "账面总金额")
    private BigDecimal totalBookAmount;

    @Schema(description = "账面总数量")
    private Integer totalBookCount;

    @Schema(description = "实际总数量")
    private Integer totalActualCount;

    @Schema(description = "盈亏总数量")
    private Integer totalDiscrepancyCount;

    @Schema(description = "盘盈总数量")
    private Integer profitCount;

    @Schema(description = "盘盈总金额")
    private BigDecimal profitAmount;

    @Schema(description = "盘亏总数量")
    private Integer lossCount;

    @Schema(description = "盘亏总金额")
    private BigDecimal lossAmount;

    @Schema(description = "实际盈亏总金额")
    private BigDecimal totalDiscrepancyAmount;

    @Schema(description = "盘库状态(0草稿 1已盘点 2已调账)")
    private Integer status;

    @Schema(description = "盘盈入库状态(0未生成 1已生成 2无需生成)")
    private Integer adjustInStatus;

    @Schema(description = "生成的盘盈入库单号")
    private String adjustInOrderNo;

    @Schema(description = "盘亏出库状态(0未生成 1已生成 2无需生成)")
    private Integer adjustOutStatus;

    @Schema(description = "生成的盘亏出库单号")
    private String adjustOutOrderNo;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "审批开关(0关闭 1开启)")
    private Integer approvalSwitch;

    @Schema(description = "商品明细列表")
    private List<WmsStocktakingDetail> details;
}
