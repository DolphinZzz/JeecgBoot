package org.jeecg.modules.wms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.modules.wms.entity.WmsWaveOrder;

import java.io.Serializable;
import java.util.List;

/**
 * @Description: 波次详情视图（包含汇总拣货行与关联出库单）
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Data
@Schema(description = "波次详情视图")
public class WmsWaveDetailView implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 波次主信息 */
    @Schema(description = "波次主信息")
    private WmsWaveOrder waveOrder;

    /** 关联的出库单清单 */
    @Schema(description = "关联的出库单清单")
    private List<WmsOutboundPage> outboundOrders;

    /** 波次汇总拣货任务清单（按库位和SKU汇总，便于批量摘果到播种车） */
    @Schema(description = "汇总拣货任务清单")
    private List<WmsWavePickItem> pickItems;

    @Data
    @Schema(description = "波次汇总拣货行")
    public static class WmsWavePickItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long skuId;
        private String skuCode;
        private String skuName;
        private String barCode;
        private String batchNumber;
        private Integer warehouseAreaLocaId;
        private String locaCode;
        private Integer totalPickCount;
    }
}
