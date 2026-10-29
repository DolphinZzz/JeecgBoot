package org.jeecg.modules.wms.vo;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description: 新建库内作业单入参（移库移位 / 补货）
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@Schema(description = "新建库内作业单")
public class WmsOperationForm {

    /**作业子类(1移库移位 2补货)，为 1 时明细的移出/移入库位必填*/
    @Schema(description = "作业子类(1移库移位 2补货)")
    private Integer operationType;

    /**仓库ID*/
    @Schema(description = "仓库ID")
    private Integer warehouseId;

    /**备注*/
    @Schema(description = "备注")
    private String remark;

    /**明细行*/
    @Schema(description = "明细行")
    private List<WmsOperationDetailForm> details;
}
