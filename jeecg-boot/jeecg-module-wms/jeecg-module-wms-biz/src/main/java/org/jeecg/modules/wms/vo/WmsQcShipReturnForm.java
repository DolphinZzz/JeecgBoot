package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description: 确认退还供应商出库表单
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "确认退还供应商出库表单")
public class WmsQcShipReturnForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 质检单ID */
    @Schema(description = "质检单ID", required = true)
    private Long qcId;

    /** 退还交接/物流单号/出库备注 */
    @Schema(description = "退还交接/物流单号/出库备注")
    private String shipRemark;
}
