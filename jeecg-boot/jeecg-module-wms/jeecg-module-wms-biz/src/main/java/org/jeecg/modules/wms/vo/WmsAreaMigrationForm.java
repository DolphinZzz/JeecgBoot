package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @Description: 整库区迁移提交表单
 * @Author: jeecg-boot
 * @Date: 2026-11-02
 */
@Data
@Schema(description = "整库区迁移提交表单")
public class WmsAreaMigrationForm {

    @Schema(description = "源仓库ID")
    private Integer sourceWarehouseId;

    @Schema(description = "源库区ID")
    private Integer sourceWarehouseAreaId;

    @Schema(description = "目标仓库ID")
    private Integer targetWarehouseId;

    @Schema(description = "目标库区ID")
    private Integer targetWarehouseAreaId;

    @Schema(description = "是否立即执行移库(默认true)")
    private Boolean autoExecute = true;

    @Schema(description = "备注说明")
    private String remark;

    @Schema(description = "迁移明细")
    private List<WmsOperationDetailForm> details;
}
