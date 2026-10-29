package org.jeecg.modules.wms.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.wms.entity.WmsWarehousingEntry;

/**
 * @Description: 入库单查询行（含仓库名、货主名、仓型与收支汇总）
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WmsEntryPage extends WmsWarehousingEntry {

    /**仓库名称*/
    private String warehouseName;

    /**仓库类型(NORMAL普通仓 INDUSTRIAL工业制造仓)，页面按它渲染超收按钮*/
    private String warehouseType;

    /**货主名称*/
    private String consignorName;

    /**明细行数*/
    private Integer detailRows;

    /**明细计划数量合计*/
    private Integer planCount;

    /**明细实收数量合计*/
    private Integer receivedTotal;

    /**已上架数量合计*/
    private Integer putawayTotal;
}
