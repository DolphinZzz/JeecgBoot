package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 库位商品规划批量保存 DTO
 * @Author: jeecg-boot
 * @Date: 2026-11-01
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "库位商品规划批量保存DTO")
public class WmsLocaCapacitySaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "库位ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer locaId;

    @Schema(description = "规划存放的商品SPU列表(最多3个)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CapacityItem> items;

    @Data
    @Accessors(chain = true)
    @Schema(description = "单个商品规划项")
    public static class CapacityItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "商品SPU ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long spuId;

        @Schema(description = "最大可存放件数", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer maxCount;

        @Schema(description = "规划备注")
        private String remark;
    }
}
