package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description: 录入实盘数
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@Schema(description = "录入实盘数")
public class WmsStocktakingCountForm {

    /**盘点记录ID*/
    @Schema(description = "盘点记录ID")
    private Long id;

    /**实盘数量*/
    @Schema(description = "实盘数量")
    private Integer correctCount;
}
