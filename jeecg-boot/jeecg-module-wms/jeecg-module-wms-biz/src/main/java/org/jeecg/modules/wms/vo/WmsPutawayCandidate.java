package org.jeecg.modules.wms.vo;

import lombok.Data;
import lombok.experimental.Accessors;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;

/**
 * @Description: 上架候选库位（策略引擎的中间结果）
 *
 * 候选对象携带排序需要的全部派生字段，排序阶段不再回表：
 * - remainVolume / remainWeight：剩余容积 / 承重
 * - capacity：按 cap_i = min(⌊剩余体积 / 单件体积⌋, ⌊剩余承重 / 单件重量⌋) 算出还能放几件
 * - sameSku：是否已存放同 SKU（不允许混批次时要求同批次）
 * - levelGap：库位等级与商品等级的差距，0 同级、1 低一级、负数表示库位等级更高
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
public class WmsPutawayCandidate {

    /**候选库位*/
    private WmsWarehouseAreaLoca loca;

    /**库区分区类型(zone_type)*/
    private String zoneType;

    /**库区类型(category)：STORAGE / PICKING / RETURN 等*/
    private String areaCategory;

    /**剩余容积（未配置容积时为 Integer.MAX_VALUE，表示不限制）*/
    private int remainVolume;

    /**剩余承重（未配置承重时为 Integer.MAX_VALUE，表示不限制）*/
    private int remainWeight;

    /**按体积与承重算出的还能放几件*/
    private int capacity;

    /**是否已存放同 SKU（不允许混批次时要求同批次）*/
    private boolean sameSku;

    /**库位等级与商品等级的差距：0 同级 / 1 低一级 / 负数 库位等级更高*/
    private int levelGap;

    /**是否固定库位溢出候选*/
    private boolean overflow;

    /**排序用的库位得分，loca_score 为空时按 0 处理*/
    private double locaScore;

    /**排序用的收货区距离，为空时排到最后*/
    private int receiveDistance;
}
