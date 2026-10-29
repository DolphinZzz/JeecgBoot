package org.jeecg.modules.wms.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Description: 上架推荐落点（一个库位放几件）
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
public class WmsPutawayPlanLine {

    /**
     * 归属的入库明细ID。
     * 策略引擎是按「单 SKU + 数量」算落点的，不关心是哪张单的哪一行；
     * 入库单把多条明细的推荐合并成一个 plan 时回填这个字段，页面上才能按明细分组展示。
     */
    private Long detailId;

    /**推荐库位ID*/
    private Integer locaId;

    /**推荐库位编码*/
    private String locaCode;

    /**库区ID*/
    private Integer areaId;

    /**库区名称*/
    private String areaName;

    /**本库位安排的件数*/
    private int count;

    /**库位剩余可放数（cap_i），页面展示「还能放多少」*/
    private int capacity;

    /**是否固定库位溢出落点（FIXED 策略下溢出到非绑定库位）*/
    private boolean overflow;

    /**是否已存放同 SKU / 同批次（同品集中）*/
    private boolean sameSku;
}
