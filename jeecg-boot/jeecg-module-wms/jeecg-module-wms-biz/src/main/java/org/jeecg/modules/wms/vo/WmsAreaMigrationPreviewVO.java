package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @Description: 整库区迁移预检结果VO
 * @Author: jeecg-boot
 * @Date: 2026-11-02
 */
@Data
@Accessors(chain = true)
@Schema(description = "整库区迁移预检结果")
public class WmsAreaMigrationPreviewVO {

    @Schema(description = "是否可执行迁移(空位充足且温区兼容)")
    private Boolean canMigrate;

    @Schema(description = "预检提示信息")
    private String message;

    @Schema(description = "源仓库ID")
    private Integer sourceWarehouseId;

    @Schema(description = "源库区ID")
    private Integer sourceWarehouseAreaId;

    @Schema(description = "源库区名称")
    private String sourceAreaName;

    @Schema(description = "目标仓库ID")
    private Integer targetWarehouseId;

    @Schema(description = "目标库区ID")
    private Integer targetWarehouseAreaId;

    @Schema(description = "目标库区名称")
    private String targetAreaName;

    @Schema(description = "源库区有货库位数")
    private Integer sourceOccupiedLocaCount;

    @Schema(description = "源库区在库总件数")
    private Integer sourceTotalItemCount;

    @Schema(description = "目标库区可用空库位数")
    private Integer targetEmptyLocaCount;

    @Schema(description = "缺少的目标库位数(缺口)")
    private Integer missingLocaCount;

    @Schema(description = "缺少目标库位的源库位编码列表")
    private List<String> missingSourceLocas;

    @Schema(description = "迁移匹配明细")
    private List<WmsAreaMigrationDetailVO> details;
}
