package org.jeecg.modules.wms.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Description: 上架推荐入参（策略引擎内部使用，不直接暴露给浏览器）
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
public class WmsPutawayRequest {

    /**仓库ID*/
    private Integer warehouseId;

    /**SKU ID*/
    private Long skuId;

    /**批次号*/
    private String batchNumber;

    /**待上架数量*/
    private int count;

    /**质量状态(1良品 2待检 3不良品)：良品进 STORAGE/PICKING，不良品只能进 RETURN*/
    private Integer qualityStatus;

    /**货主ID，参与混放判断的库存过滤*/
    private Long consignorId;
}
