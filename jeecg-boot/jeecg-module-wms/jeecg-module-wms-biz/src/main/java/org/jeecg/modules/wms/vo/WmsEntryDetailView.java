package org.jeecg.modules.wms.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.wms.entity.WmsWarehousingEntryDetails;

/**
 * @Description: 入库明细查询行（含商品编码 / 名称 / 已上架数 / SN 管理标识）
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WmsEntryDetailView extends WmsWarehousingEntryDetails {

    /**商品SKU编码*/
    private String skuCode;

    /**商品名称（SPU 标题）*/
    private String spuTitle;

    /**是否SN管理(1是 0否)：收货页面据此决定是否要求扫码*/
    private Integer isSnManaged;

    /**已上架数量*/
    private Integer putawayCount;

    /**默认质检方式(1免检 2抽检 3全检)*/
    private Integer defaultQcType;
}
