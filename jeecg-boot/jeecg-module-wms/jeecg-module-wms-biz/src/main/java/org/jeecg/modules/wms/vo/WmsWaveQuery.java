package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description: 波次查询条件
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "波次查询条件")
public class WmsWaveQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 波次号 */
    @Schema(description = "波次号")
    private String waveCode;

    /** 仓库ID */
    @Schema(description = "仓库ID")
    private Integer warehouseId;

    /** 拣货模式 */
    @Schema(description = "拣货模式(1摘果法 2波次播种法)")
    private Integer pickMode;

    /** 承运商ID */
    @Schema(description = "承运商ID")
    private Long logisticsCompanyId;

    /** 存储温区 */
    @Schema(description = "存储温区")
    private String storageCondition;

    /** 状态 */
    @Schema(description = "状态")
    private Integer status;
}
