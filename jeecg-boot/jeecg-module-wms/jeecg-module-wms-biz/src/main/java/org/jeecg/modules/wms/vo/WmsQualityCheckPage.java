package org.jeecg.modules.wms.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.wms.entity.WmsQualityCheck;

/**
 * @Description: 质检单查询行（含入库单号、仓库名、商品编码与名称）
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WmsQualityCheckPage extends WmsQualityCheck {

    /**入库单号*/
    private String entryCode;

    /**入库类型(1采购入库 2MES生产入库 3销退入库 4调拨入库)*/
    private Integer entryType;

    /**仓库ID*/
    private Integer warehouseId;

    /**仓库名称*/
    private String warehouseName;

    /**仓库类型(NORMAL普通仓 INDUSTRIAL工业制造仓)*/
    private String warehouseType;

    /**商品SKU编码*/
    private String skuCode;

    /**商品名称（SPU 标题）*/
    private String spuTitle;

    /**该明细的实收数量，页面用来提示送检上限*/
    private Integer receivedCount;
}
