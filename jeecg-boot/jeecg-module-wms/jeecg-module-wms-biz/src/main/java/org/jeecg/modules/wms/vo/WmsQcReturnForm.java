package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description: 质检不合格品退货处理表单（放置退货货架区并联系供应商）
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "质检不合格品退货处理表单")
public class WmsQcReturnForm implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 质检单ID */
    @Schema(description = "质检单ID", required = true)
    private Long qcId;

    /** 退货放置库位ID（必须为退货不良区 RETURN 的库位） */
    @Schema(description = "退货放置库位ID", required = true)
    private Integer returnLocaId;

    /** 退货联系处置备注/沟通说明 */
    @Schema(description = "退货联系处置备注/沟通说明")
    private String returnRemark;
}
