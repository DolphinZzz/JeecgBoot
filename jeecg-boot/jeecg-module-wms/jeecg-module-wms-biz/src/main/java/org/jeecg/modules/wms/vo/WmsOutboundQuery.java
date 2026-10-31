package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description: 出库单查询条件
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "出库单查询条件")
public class WmsOutboundQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 出库单号 */
    @Schema(description = "出库单号")
    private String outboundCode;

    /** 出库类型 */
    @Schema(description = "出库类型(1销售出库 2MES工单领料 3采购退货 4调拨出库)")
    private Integer outboundType;

    /** 拣货模式 */
    @Schema(description = "拣货模式(1摘果法 2波次播种法)")
    private Integer pickMode;

    /** 出库状态 */
    @Schema(description = "出库状态")
    private Integer status;

    /** 仓库ID */
    @Schema(description = "仓库ID")
    private Integer warehouseId;

    /** 货主ID */
    @Schema(description = "货主ID")
    private Long consignorId;

    /** 客户ID */
    @Schema(description = "客户ID")
    private Long customerId;

    /** 波次ID */
    @Schema(description = "波次ID")
    private Long waveId;

    /** 上游ERP单号 */
    @Schema(description = "上游ERP单号")
    private String erpOrderNo;

    /** MES生产工单号 */
    @Schema(description = "MES生产工单号")
    private String mesWorkOrderNo;

    /** 物流单号 */
    @Schema(description = "物流单号")
    private String logisticsNumber;
}
