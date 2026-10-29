package org.jeecg.modules.wms.vo;

import lombok.Data;

/**
 * @Description: 多维库存查询条件
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Data
public class WmsInventoryQuery {

    /**仓库ID*/
    private Integer warehouseId;

    /**库区ID*/
    private Integer warehouseAreaId;

    /**库位ID*/
    private Integer warehouseAreaLocaId;

    /**货主ID*/
    private Long consignorId;

    /**SKU ID*/
    private Long skuId;

    /**SKU 编码（模糊）*/
    private String skuCode;

    /**商品名称（模糊）*/
    private String spuTitle;

    /**批次号（精确）*/
    private String batchNumber;

    /**质量状态(1良品 2待检 3不良品)*/
    private Integer qualityStatus;

    /**存储条件*/
    private String storageCondition;

    /**库位编号（模糊）*/
    private String locaCode;

    /**效期状态(1正常 2临期 3过期 4无有效期)*/
    private Integer expireStatus;

    /**
     * SN 序列号（模糊）。SN 记在 wms_item_sn 上，按 SN 查库存时先由 SN
     * 定位到 (库位, SKU, 批次) 组合，再回库存表取数量。
     */
    private String snCode;

    /**只看有库存的行（total_count > 0），默认只看有货的行*/
    private Boolean onlyAvailable;
}
