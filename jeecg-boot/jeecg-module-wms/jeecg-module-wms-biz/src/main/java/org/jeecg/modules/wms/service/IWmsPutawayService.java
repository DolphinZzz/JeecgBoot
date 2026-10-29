package org.jeecg.modules.wms.service;

import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.vo.WmsPutawayPlan;
import org.jeecg.modules.wms.vo.WmsPutawayRequest;

/**
 * @Description: 上架策略引擎（方案 3.5 的三层结构）
 *
 * 第 1 层硬过滤（所有策略共用，任何策略都绕不过）：
 *   库位状态未禁用 / 未盘点锁定 → 质量状态对应的库区类型 → 存储条件 →
 *   分区 zone_type（含兜底）→ 混放规则 → 剩余容积与承重。
 * 第 2 层排序：按 SKU 的 putaway_strategy（为空取仓库默认 ABC）执行
 *   FIXED / ABC / NEAREST 三种排序。
 * 第 3 层拆分：按 cap_i = min(⌊剩余体积/单件体积⌋, ⌊剩余承重/单件重量⌋)
 *   依次填满，放不完的部分记 remainCount。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
public interface IWmsPutawayService {

    /**
     * 计算推荐落点清单。不落库，只出方案，供页面展示和确认。
     */
    WmsPutawayPlan recommend(WmsPutawayRequest request);

    /**
     * 校验人工指定的目标库位是否通过第 1 层硬过滤。
     * 人工偏离推荐时用：通过即允许上架，由调用方记录 is_deviate。
     *
     * @throws org.jeecg.common.exception.JeecgBootException 未通过时抛出具体原因
     */
    WmsWarehouseAreaLoca checkTargetLoca(WmsPutawayRequest request, Integer locaId);
}
