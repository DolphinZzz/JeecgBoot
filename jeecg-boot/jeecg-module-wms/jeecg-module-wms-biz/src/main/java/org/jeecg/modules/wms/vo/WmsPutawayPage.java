package org.jeecg.modules.wms.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.wms.entity.WmsWarehousingEntryUp;

/**
 * @Description: 上架记录查询行（含单号、库位编码、商品编码与名称、推荐库位编码）
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WmsPutawayPage extends WmsWarehousingEntryUp {

    /**入库单号*/
    private String entryCode;

    /**仓库名称*/
    private String warehouseName;

    /**库区名称*/
    private String areaName;

    /**实际上架库位编码*/
    private String locaCode;

    /**系统推荐库位编码（推荐为当前库位时与 locaCode 相同）*/
    private String recommendLocaCode;

    /**商品SKU编码*/
    private String skuCode;

    /**商品名称（SPU 标题）*/
    private String spuTitle;

    /**该明细的实收数量*/
    private Integer receivedCount;
}
