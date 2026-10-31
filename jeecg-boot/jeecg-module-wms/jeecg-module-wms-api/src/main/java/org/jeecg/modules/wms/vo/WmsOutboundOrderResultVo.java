package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Description: 创建出库单返回结果 VO
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "创建出库单结果VO")
public class WmsOutboundOrderResultVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "出库单ID")
    private Long id;

    @Schema(description = "出库单号")
    private String outboundCode;

    @Schema(description = "出库单状态")
    private Integer status;

    @Schema(description = "总件数")
    private Integer totalCount;

    @Schema(description = "明细行数")
    private Integer detailRows;
}
