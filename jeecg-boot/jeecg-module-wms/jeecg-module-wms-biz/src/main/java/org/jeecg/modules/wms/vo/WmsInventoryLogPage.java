package org.jeecg.modules.wms.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.wms.entity.WmsInventoryLog;

/**
 * @Description: 库存流水查询行（含仓库/库位/SKU 编码）
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WmsInventoryLogPage extends WmsInventoryLog {

    /**仓库名称*/
    private String warehouseName;

    /**库区名称*/
    private String areaName;

    /**库位编号*/
    private String locaCode;

    /**商品SKU编码*/
    private String skuCode;

    /**商品名称（SPU 标题）*/
    private String spuTitle;

    /**货主名称*/
    private String consignorName;
}
