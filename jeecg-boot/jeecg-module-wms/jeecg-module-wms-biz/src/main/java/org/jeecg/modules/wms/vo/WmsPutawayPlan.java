package org.jeecg.modules.wms.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Description: 上架推荐结果（第 3 层数量拆分后的落点清单）
 *
 * plan 里每一条 = 一个「放哪个库位、放几件」，按推荐优先级排列；
 * 放不完的剩余量记在 remainCount，页面提示人工处理，不静默丢失。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Data
@Accessors(chain = true)
public class WmsPutawayPlan {

    /**本次使用的排序策略(FIXED / ABC / NEAREST)*/
    private String strategyUsed;

    /**商品推导出的分区类型(GENERAL / SMALL / LARGE / VALUABLE)*/
    private String zoneType;

    /**待上架数量*/
    private int needCount;

    /**未能安排的数量（无可放库位），0 表示全部排上了*/
    private int remainCount;

    /**落点清单，按推荐优先级排列*/
    private List<WmsPutawayPlanLine> lines = new ArrayList<>();

    /**排上的数量合计*/
    public int plannedCount() {
        return lines.stream().mapToInt(WmsPutawayPlanLine::getCount).sum();
    }

    public WmsPutawayPlan addLine(WmsPutawayPlanLine line) {
        this.lines.add(line);
        return this;
    }
}
