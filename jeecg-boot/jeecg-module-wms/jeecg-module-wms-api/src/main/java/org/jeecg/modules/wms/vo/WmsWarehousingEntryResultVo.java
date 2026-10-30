package org.jeecg.modules.wms.vo;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Description: 入库单创建结果响应 VO
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "入库单创建结果")
public class WmsWarehousingEntryResultVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "入库单主键ID")
    private Long entryId;

    @Schema(description = "WMS入库单号")
    private String entryCode;

    @Schema(description = "ERP采购单号")
    private String erpPoNo;

    @Schema(description = "自动生成或指定的批次号")
    private String batchNumber;

    @Schema(description = "入库单状态")
    private Integer status;

    @Schema(description = "计划入库总件数")
    private Integer totalCount;
}
