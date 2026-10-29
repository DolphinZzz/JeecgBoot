package org.jeecg.modules.wms.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.wms.entity.WmsInventory;

/**
 * @Description: 多维库存查询行（含仓库/库区/库位/SKU 编码与效期状态）
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WmsInventoryPage extends WmsInventory {

    /**仓库名称*/
    private String warehouseName;

    /**库区名称*/
    private String areaName;

    /**库位编号*/
    private String locaCode;

    /**库位存储条件*/
    private String storageCondition;

    /**商品SKU编码*/
    private String skuCode;

    /**商品名称（SPU 标题）*/
    private String spuTitle;

    /**货主名称*/
    private String consignorName;

    /**效期状态(1正常 2临期 3过期 4无有效期)*/
    private Integer expireStatus;
}
