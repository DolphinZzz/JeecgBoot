package org.jeecg.modules.wms.vo;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description: 提交质检结果入参（一次可提交多张质检单）
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@Schema(description = "提交质检结果")
public class WmsQcSubmitForm {

    /**质检单ID列表（按提交的顺序逐个落质检结论）*/
    @Schema(description = "质检单ID列表")
    private List<Long> qcIds;

    /**质检员工ID*/
    @Schema(description = "质检员工ID")
    private Integer qcEmpId;

    /**不合格原因（本次不合格的质检单共用，可为空）*/
    @Schema(description = "不合格原因")
    private String failReason;

    /**明细结果，为空则按 qcType 自动判定（免检全合格、其余按抽检结果）*/
    @Schema(description = "明细结果")
    private List<WmsQcSubmitLine> lines;

    @Data
    @Schema(description = "单张质检单的提交结果")
    public static class WmsQcSubmitLine {

        /**质检单ID*/
        @Schema(description = "质检单ID")
        private Long qcId;

        /**质检方式(1免检 2抽检 3全检)，为空按商品默认*/
        @Schema(description = "质检方式(1免检 2抽检 3全检)")
        private Integer qcType;

        /**抽检数量，为空按质检方式默认值*/
        @Schema(description = "抽检数量")
        private Integer sampleCount;

        /**合格数量*/
        @Schema(description = "合格数量")
        private Integer passCount;

        /**不合格数量*/
        @Schema(description = "不合格数量")
        private Integer failCount;

        /**不合格原因*/
        @Schema(description = "不合格原因")
        private String failReason;
    }
}
