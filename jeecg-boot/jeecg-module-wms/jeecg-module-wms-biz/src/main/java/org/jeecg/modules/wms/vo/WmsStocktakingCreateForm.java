package org.jeecg.modules.wms.vo;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Description: 生成盘点任务入参
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@Schema(description = "生成盘点任务")
public class WmsStocktakingCreateForm {

    /**仓库ID（必填）*/
    @Schema(description = "仓库ID")
    private Integer warehouseId;

    /**库区ID（可空，空表示全仓）*/
    @Schema(description = "库区ID")
    private Integer warehouseAreaId;

    /**库位ID列表（可空，空表示按库区/仓库范围）*/
    @Schema(description = "库位ID列表")
    private List<Integer> locaIds;

    /**盘点类型(1滚动盘点 2定时盘点)*/
    @Schema(description = "盘点类型(1滚动盘点 2定时盘点)")
    private Integer category;

    /**是否只盘有库存的库位（默认 true）*/
    @Schema(description = "是否只盘有库存的库位")
    private Boolean onlyOccupied;
}
