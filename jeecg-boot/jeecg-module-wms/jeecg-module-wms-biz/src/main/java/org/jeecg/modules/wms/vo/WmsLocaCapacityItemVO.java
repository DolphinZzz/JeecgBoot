package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @Description: 库位已配置商品规划 VO
 * @Author: jeecg-boot
 * @Date: 2026-11-01
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "库位已配置商品规划VO")
public class WmsLocaCapacityItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "规划记录ID")
    private Long id;

    @Schema(description = "商品SPU ID")
    private Long spuId;

    @Schema(description = "商品编码")
    private String spuCode;

    @Schema(description = "商品名称")
    private String spuName;

    @Schema(description = "最大可存放件数")
    private Integer maxCount;

    @Schema(description = "当前在库件数")
    private Integer currentCount;

    @Schema(description = "剩余可放件数")
    private Integer remainCount;

    @Schema(description = "规划备注")
    private String remark;
}
