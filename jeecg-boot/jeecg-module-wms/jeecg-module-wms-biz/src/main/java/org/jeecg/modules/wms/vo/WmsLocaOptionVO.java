package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Description: 库位选项简要VO
 * @Author: jeecg-boot
 * @Date: 2026-11-02
 */
@Data
@Accessors(chain = true)
@Schema(description = "库位选项")
public class WmsLocaOptionVO {
    @Schema(description = "库位ID")
    private Integer locaId;

    @Schema(description = "库位编码")
    private String locaCode;
}
