package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsItemSpu;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.enums.WmsPutawayStrategyEnum;
import org.jeecg.modules.wms.enums.WmsZoneTypeEnum;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.mapper.WmsItemSkuMapper;
import org.jeecg.modules.wms.mapper.WmsItemSpuMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.service.IWmsPutawayService;
import org.jeecg.modules.wms.service.IWmsStrategyConfigService;
import org.jeecg.modules.wms.vo.WmsPutawayCandidate;
import org.jeecg.modules.wms.vo.WmsPutawayLocaOption;
import org.jeecg.modules.wms.vo.WmsPutawayPlan;
import org.jeecg.modules.wms.vo.WmsPutawayPlanLine;
import org.jeecg.modules.wms.vo.WmsPutawayRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Description: 上架策略引擎实现
 *
 * 三层结构严格分离：硬过滤只管「能不能放」，排序只管「先放哪个」，
 * 拆分只管「各放几件」。任何一层出问题都只影响本层，互不串味。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-30
 * @Version: V1.0
 */
@Slf4j
@Service
public class WmsPutawayServiceImpl implements IWmsPutawayService {

    /** 库位状态：盘点锁定 */
    private static final int LOCA_STATUS_STOCKTAKING = 3;
    /** 库位状态：禁用 */
    private static final int LOCA_STATUS_DISABLED = 4;
    /** 库区类型：存储区（良品可上） */
    private static final String AREA_STORAGE = "STORAGE";
    /** 库区类型：拣货区（良品可上） */
    private static final String AREA_PICKING = "PICKING";
    /** 库区类型：退货不良区（不良品只能上这里，IN-03） */
    private static final String AREA_RETURN = "RETURN";
    /** 默认存储条件 */
    private static final String DEFAULT_STORAGE_CONDITION = "NORMAL";
    /** 质量状态：良品 */
    private static final int QUALITY_GOOD = 1;
    /** 未配置容积 / 承重时的「不限制」值 */
    private static final int UNLIMITED = Integer.MAX_VALUE / 4;
    /** 距离为空时的兜底值，保证无距离数据的库位排在最后 */
    private static final int DISTANCE_UNKNOWN = 999999;

    @Autowired
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Autowired
    private WmsWarehouseAreaMapper areaMapper;

    @Autowired
    private WmsItemSkuMapper skuMapper;

    @Autowired
    private WmsItemSpuMapper spuMapper;

    @Autowired
    private WmsInventoryMapper inventoryMapper;

    @Autowired
    private IWmsStrategyConfigService strategyConfigService;

    @Override
    public WmsPutawayPlan recommend(WmsPutawayRequest request) {
        validateRequest(request);
        WmsItemSku sku = skuMapper.selectById(request.getSkuId());
        if (sku == null) {
            throw new JeecgBootException("商品SKU不存在");
        }
        WmsItemSpu spu = sku.getSpuId() == null ? null : spuMapper.selectById(sku.getSpuId());
        int warehouseId = request.getWarehouseId();

        // 商品分区推导：贵品 → 大件 → 小件 → 通用
        WmsZoneTypeEnum itemZone = deriveItemZone(sku, spu, warehouseId);

        // 第 1 层：硬过滤出候选库位（顺带统计被淘汰的原因，供页面说明「为什么放不下」）
        LinkedHashMap<String, Integer> rejected = new LinkedHashMap<>();
        List<WmsPutawayCandidate> candidates = filterCandidates(request, sku, spu, itemZone, rejected);
        if (candidates.isEmpty()) {
            WmsPutawayPlan empty = new WmsPutawayPlan()
                    .setStrategyUsed(resolveStrategy(sku, warehouseId).getCode())
                    .setZoneType(itemZone.getCode())
                    .setNeedCount(request.getCount())
                    .setRemainCount(request.getCount())
                    .setRejectedCount(rejected.values().stream().mapToInt(Integer::intValue).sum())
                    .setEmptyReason(summarizeRejected(rejected, itemZone));
            log.info("SKU {} 在仓库 {} 无可用上架库位（分区 {}，原因 {}）",
                    request.getSkuId(), warehouseId, itemZone.getCode(), empty.getEmptyReason());
            return empty;
        }

        // 第 2 层 + 第 3 层：按策略排序后依次填满
        WmsPutawayStrategyEnum strategy = resolveStrategy(sku, warehouseId);
        WmsPutawayPlan plan = new WmsPutawayPlan()
                .setStrategyUsed(strategy.getCode())
                .setZoneType(itemZone.getCode())
                .setNeedCount(request.getCount());
        int remain = request.getCount();
        if (strategy == WmsPutawayStrategyEnum.FIXED) {
            remain = planFixed(candidates, plan, remain, sku, warehouseId);
        } else if (strategy == WmsPutawayStrategyEnum.NEAREST) {
            remain = planNearest(candidates, plan, remain, warehouseId);
        } else {
            remain = planAbc(candidates, plan, remain, sku);
        }
        plan.setRemainCount(Math.max(0, remain));
        return plan;
    }

    @Override
    public WmsWarehouseAreaLoca checkTargetLoca(WmsPutawayRequest request, Integer locaId) {
        validateRequest(request);
        if (locaId == null) {
            throw new JeecgBootException("请指定目标库位");
        }
        WmsItemSku sku = skuMapper.selectById(request.getSkuId());
        if (sku == null) {
            throw new JeecgBootException("商品SKU不存在");
        }
        WmsItemSpu spu = sku.getSpuId() == null ? null : spuMapper.selectById(sku.getSpuId());
        WmsWarehouseAreaLoca loca = locaMapper.selectById(locaId);
        if (loca == null) {
            throw new JeecgBootException("目标库位不存在");
        }
        WmsZoneTypeEnum itemZone = deriveItemZone(sku, spu, request.getWarehouseId());
        // 单库位校验：逐个断言，抛出的原因就是页面上看到的原因
        assertLocaAllowed(request, sku, spu, itemZone, loca,
                loadArea(loca.getWarehouseAreaId()), loadOccupied(locaId), request.getCount());
        return loca;
    }

    // ------------------------------------------------------------------
    // 第 1 层：硬过滤
    // ------------------------------------------------------------------

    /**
     * 遍历仓库内所有参与上架的库区下的库位，逐个走硬过滤。
     *
     * 只扫本仓库的 STORAGE / PICKING / RETURN 库区：收货区、待检区、集货区
     * 天然不是上架落点，扫进来只是浪费查询（方案 3.5.4 也写明这些区不参与）。
     */
    private List<WmsPutawayCandidate> filterCandidates(WmsPutawayRequest request, WmsItemSku sku,
                                                       WmsItemSpu spu, WmsZoneTypeEnum itemZone,
                                                       LinkedHashMap<String, Integer> rejected) {
        List<WmsWarehouseArea> areas = areaMapper.selectList(new LambdaQueryWrapper<WmsWarehouseArea>()
                .eq(WmsWarehouseArea::getWarehouseId, request.getWarehouseId())
                .in(WmsWarehouseArea::getCategory, AREA_STORAGE, AREA_PICKING, AREA_RETURN));
        if (areas.isEmpty()) {
            rejected.merge("本仓没有存储区/拣货区/退货不良区，需要先建库区", 1, Integer::sum);
            return new ArrayList<>();
        }
        Map<Integer, WmsWarehouseArea> areaMap = areas.stream()
                .collect(Collectors.toMap(WmsWarehouseArea::getId, a -> a, (a, b) -> a));
        List<WmsWarehouseAreaLoca> locas = locaMapper.selectList(new LambdaQueryWrapper<WmsWarehouseAreaLoca>()
                .eq(WmsWarehouseAreaLoca::getWarehouseId, request.getWarehouseId())
                .in(WmsWarehouseAreaLoca::getWarehouseAreaId, areaMap.keySet())
                .orderByAsc(WmsWarehouseAreaLoca::getLocaCode));
        if (locas.isEmpty()) {
            rejected.merge("本仓的存储区/拣货区/退货不良区下还没有库位，需要先建库位", 1, Integer::sum);
            return new ArrayList<>();
        }

        // 一次性取这些库位的占用库存，避免逐库位查询
        List<Integer> locaIds = locas.stream().map(WmsWarehouseAreaLoca::getId).collect(Collectors.toList());
        Map<Integer, List<WmsInventory>> occupiedMap = loadOccupiedByLocas(locaIds);

        String itemLevel = resolveItemLevel(sku, spu, request.getWarehouseId());
        boolean allowUpgradeOccupy = Boolean.TRUE.equals(strategyConfigService.getBooleanConfig(
                request.getWarehouseId(), "putaway_allow_upgrade_occupy", false));

        List<WmsPutawayCandidate> result = new ArrayList<>();
        for (WmsWarehouseAreaLoca loca : locas) {
            WmsWarehouseArea area = areaMap.get(loca.getWarehouseAreaId());
            List<WmsInventory> occupied = occupiedMap.getOrDefault(loca.getId(), new ArrayList<>());
            try {
                // 候选枚举阶段只要求「至少放得下 1 件」：本批数量由第 3 层按 cap_i 拆分，
                // 这里按整批校验会把所有放不满整批的库位都筛掉，永远拆不起来。
                // 人工指定库位（checkTargetLoca）才按整批校验，因为那是明确的一次落库。
                assertLocaAllowed(request, sku, spu, itemZone, loca, area, occupied, 1);
            } catch (JeecgBootException e) {
                // 硬过滤阶段被排除是正常结果，不是错误，记 debug 便于排查「为什么没推荐这个库位」
                log.debug("库位 {} 被硬过滤：{}", loca.getLocaCode(), e.getMessage());
                // 原因去掉库位编码后归并计数，同一个原因只留一条，页面按条数排序展示
                rejected.merge(reasonOf(e.getMessage(), loca.getLocaCode()), 1, Integer::sum);
                continue;
            }
            WmsPutawayCandidate candidate = buildCandidate(request, sku, loca, area, occupied);
            candidate.setLevelGap(levelGap(loca.getAbcClass(), itemLevel));
            if (!allowUpgradeOccupy && candidate.getLevelGap() < 0) {
                // 比商品等级高的库位默认不可占用（putaway_allow_upgrade_occupy = 0）
                rejected.merge("库位等级高于商品等级，且未开启越级占用", 1, Integer::sum);
                continue;
            }
            result.add(candidate);
        }
        return result;
    }

    /**
     * 单个库位的硬过滤断言，按方案 3.5.1 的顺序逐条校验。
     * 抛出的异常消息就是页面上展示的拒绝原因。
     */
    private void assertLocaAllowed(WmsPutawayRequest request, WmsItemSku sku, WmsItemSpu spu,
                                   WmsZoneTypeEnum itemZone, WmsWarehouseAreaLoca loca,
                                   WmsWarehouseArea area, List<WmsInventory> occupied, int count) {
        if (loca == null) {
            throw new JeecgBootException("目标库位不存在");
        }
        String locaCode = loca.getLocaCode();

        // ① 库位状态：未禁用、未盘点锁定
        if (loca.getLocaStatus() != null && loca.getLocaStatus() == LOCA_STATUS_DISABLED) {
            throw new JeecgBootException("库位[" + locaCode + "]已禁用");
        }
        if (loca.getLocaStatus() != null && loca.getLocaStatus() == LOCA_STATUS_STOCKTAKING) {
            throw new JeecgBootException("库位[" + locaCode + "]正在盘点锁定，不能上架");
        }

        // ② 质量状态 → 库区类型：不良品只能进 RETURN（IN-03，全仓型生效）
        String category = area == null || area.getCategory() == null ? "" : area.getCategory();
        int qualityStatus = request.getQualityStatus() == null ? QUALITY_GOOD : request.getQualityStatus();
        if (qualityStatus == QUALITY_GOOD) {
            if (!AREA_STORAGE.equals(category) && !AREA_PICKING.equals(category)) {
                throw new JeecgBootException(String.format(
                        "良品只能上架到存储区或拣货区，库位[%s]所在库区类型为 %s", locaCode, category));
            }
        } else {
            if (!AREA_RETURN.equals(category)) {
                throw new JeecgBootException(String.format(
                        "不良品只能上架到退货不良区(RETURN)，库位[%s]所在库区类型为 %s", locaCode, category));
            }
        }

        // ③ 存储条件匹配（不允许降级）。以库区的温区为准，库位自己没配时再退回库区，
        // 两者都没配按常温；库位单独配了则以库位为准，支持同库区内做特殊温区。
        String required = resolveStorageCondition(spu);
        String locaCondition = resolveLocaStorageCondition(loca, area);
        if (!required.equals(locaCondition)) {
            throw new JeecgBootException(String.format(
                    "库位[%s]存储条件为 %s，与商品要求的 %s 不匹配", locaCode, locaCondition, required));
        }

        // ④ 分区匹配（含兜底）
        WmsZoneTypeEnum areaZone = WmsZoneTypeEnum.of(area == null ? null : area.getZoneType());
        if (!matchesZone(itemZone, areaZone)) {
            throw new JeecgBootException(String.format(
                    "库位[%s]所在库区分区为 %s，与商品分区 %s 不匹配",
                    locaCode, areaZone.getDesc(), itemZone.getDesc()));
        }

        // ⑤ 固定库位只放绑定的 SKU（fixed_loca_lend_when_empty = 0 时，空着也不借）
        boolean lendWhenEmpty = Boolean.TRUE.equals(strategyConfigService.getBooleanConfig(
                request.getWarehouseId(), "fixed_loca_lend_when_empty", false));
        if (loca.getFixedSkuId() != null && !loca.getFixedSkuId().equals(request.getSkuId())) {
            boolean empty = occupied.stream().noneMatch(row -> countOf(row.getTotalCount()) > 0);
            if (!(lendWhenEmpty && empty)) {
                throw new JeecgBootException("库位[" + locaCode + "]是固定库位，只能存放绑定的SKU");
            }
        }

        // ⑥ 混放规则
        String batch = request.getBatchNumber() == null ? "" : request.getBatchNumber();
        for (WmsInventory row : occupied) {
            if (countOf(row.getTotalCount()) <= 0) {
                continue;
            }
            if (row.getSkuId() != null && !row.getSkuId().equals(request.getSkuId())
                    && (loca.getAllowMixSku() == null || loca.getAllowMixSku() != 1)) {
                throw new JeecgBootException("库位[" + locaCode + "]不允许混放SKU");
            }
            String rowBatch = row.getBatchNumber() == null ? "" : row.getBatchNumber();
            if (!rowBatch.equals(batch)
                    && (loca.getAllowMixBatch() == null || loca.getAllowMixBatch() != 1)) {
                throw new JeecgBootException("库位[" + locaCode + "]不允许混放批次");
            }
        }

        // ⑦ 剩余容积与剩余承重（未配置时跳过该项，IN-05）
        int needVolume = unitVolume(sku) * count;
        int needWeight = unitWeight(sku) * count;
        int localVolume = countOf(loca.getLocalVolume());
        int localBearing = countOf(loca.getLocalBearingCapacity());
        int usedVolume = countOf(loca.getUsedVolume());
        int usedWeight = countOf(loca.getUsedWeight());
        if (localVolume > 0 && usedVolume + needVolume > localVolume) {
            throw new JeecgBootException(String.format(
                    "库位[%s]容积不足：剩余 %d，本次需要 %d", locaCode, localVolume - usedVolume, needVolume));
        }
        if (localBearing > 0 && usedWeight + needWeight > localBearing) {
            throw new JeecgBootException(String.format(
                    "库位[%s]承重不足：剩余 %d，本次需要 %d", locaCode, localBearing - usedWeight, needWeight));
        }
    }

    /**
     * 把硬过滤通过的库位包装成候选，预先算好排序与拆分要用的派生字段。
     */
    private WmsPutawayCandidate buildCandidate(WmsPutawayRequest request, WmsItemSku sku,
                                               WmsWarehouseAreaLoca loca, WmsWarehouseArea area,
                                               List<WmsInventory> occupied) {
        int unitVolume = unitVolume(sku);
        int unitWeight = unitWeight(sku);
        int localVolume = countOf(loca.getLocalVolume());
        int localBearing = countOf(loca.getLocalBearingCapacity());
        int remainVolume = localVolume > 0
                ? Math.max(0, localVolume - countOf(loca.getUsedVolume())) : UNLIMITED;
        int remainWeight = localBearing > 0
                ? Math.max(0, localBearing - countOf(loca.getUsedWeight())) : UNLIMITED;

        // cap_i = min(⌊剩余体积/单件体积⌋, ⌊剩余承重/单件重量⌋)；单件为空按 0 处理即不设上限
        int volumeCap = unitVolume > 0 && remainVolume < UNLIMITED
                ? remainVolume / unitVolume : UNLIMITED;
        int weightCap = unitWeight > 0 && remainWeight < UNLIMITED
                ? remainWeight / unitWeight : UNLIMITED;

        return new WmsPutawayCandidate()
                .setLoca(loca)
                .setZoneType(area == null ? null : area.getZoneType())
                .setAreaCategory(area == null ? null : area.getCategory())
                .setRemainVolume(remainVolume)
                .setRemainWeight(remainWeight)
                .setCapacity(Math.min(volumeCap, weightCap))
                .setSameSku(hasSameSku(occupied, request))
                .setOverflow(false)
                .setLocaScore(loca.getLocaScore() == null ? 0D : loca.getLocaScore().doubleValue())
                .setReceiveDistance(loca.getReceiveDistance() == null ? DISTANCE_UNKNOWN : loca.getReceiveDistance());
    }

    /** 库位里是否已存放同 SKU（不允许混批次时要求同批次且同质量状态） */
    private boolean hasSameSku(List<WmsInventory> occupied, WmsPutawayRequest request) {
        String batch = request.getBatchNumber() == null ? "" : request.getBatchNumber();
        int quality = request.getQualityStatus() == null ? QUALITY_GOOD : request.getQualityStatus();
        for (WmsInventory row : occupied) {
            if (countOf(row.getTotalCount()) <= 0) {
                continue;
            }
            if (!request.getSkuId().equals(row.getSkuId())) {
                continue;
            }
            String rowBatch = row.getBatchNumber() == null ? "" : row.getBatchNumber();
            int rowQuality = row.getQualityStatus() == null ? QUALITY_GOOD : row.getQualityStatus();
            if (rowBatch.equals(batch) && rowQuality == quality) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // 第 2 层 + 第 3 层：排序与拆分
    // ------------------------------------------------------------------

    /**
     * FIXED：先填绑定库位（按 loca_code），放不下的部分按 fixed_overflow_strategy
     * （默认 ABC）继续推荐，并标记 overflow。
     */
    private int planFixed(List<WmsPutawayCandidate> candidates, WmsPutawayPlan plan, int remain,
                          WmsItemSku sku, Integer warehouseId) {
        List<WmsPutawayCandidate> fixed = candidates.stream()
                .filter(c -> c.getLoca().getFixedSkuId() != null
                        && c.getLoca().getFixedSkuId().equals(sku.getId()))
                .sorted(Comparator.comparing(c -> c.getLoca().getLocaCode(), Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
        remain = fill(fixed, plan, remain);

        if (remain > 0) {
            // 固定库位放不下，按溢出策略继续
            String overflowStrategy = strategyConfigService.getConfigValue(
                    warehouseId, "fixed_overflow_strategy", WmsPutawayStrategyEnum.ABC.getCode());
            Set<Integer> used = fixed.stream().map(c -> c.getLoca().getId()).collect(Collectors.toSet());
            List<WmsPutawayCandidate> overflowCandidates = candidates.stream()
                    .filter(c -> !used.contains(c.getLoca().getId()))
                    .peek(c -> c.setOverflow(true))
                    .collect(Collectors.toList());
            WmsPutawayStrategyEnum overflow = WmsPutawayStrategyEnum.of(overflowStrategy);
            if (overflow == WmsPutawayStrategyEnum.NEAREST) {
                remain = planNearest(overflowCandidates, plan, remain, warehouseId);
            } else {
                remain = planAbc(overflowCandidates, plan, remain, sku);
            }
        }
        return remain;
    }

    /**
     * ABC：等级匹配 → 同品集中 → 库位得分降序 → 容量贴合（剩余容量越小越优先，
     * 避免一个批次被摊到很多库位）。
     */
    private int planAbc(List<WmsPutawayCandidate> candidates, WmsPutawayPlan plan,
                        int remain, WmsItemSku sku) {
        List<WmsPutawayCandidate> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator
                .comparingInt(WmsPutawayCandidate::getLevelGap)
                .thenComparing(c -> c.isSameSku() ? 0 : 1)
                .thenComparing(Comparator.comparingDouble(WmsPutawayCandidate::getLocaScore).reversed())
                .thenComparingInt(WmsPutawayCandidate::getCapacity));
        return fill(sorted, plan, remain);
    }

    /**
     * NEAREST：同品集中（可关）→ 离收货区距离升序。
     */
    private int planNearest(List<WmsPutawayCandidate> candidates, WmsPutawayPlan plan,
                            int remain, Integer warehouseId) {
        boolean preferSameSku = Boolean.TRUE.equals(strategyConfigService.getBooleanConfig(
                warehouseId, "putaway_prefer_same_sku", true));
        List<WmsPutawayCandidate> sorted = new ArrayList<>(candidates);
        Comparator<WmsPutawayCandidate> byDistance =
                Comparator.comparingInt(WmsPutawayCandidate::getReceiveDistance)
                        .thenComparing(c -> c.getLoca().getLocaCode(), Comparator.nullsLast(String::compareTo));
        if (preferSameSku) {
            sorted.sort(Comparator.<WmsPutawayCandidate>comparingInt(c -> c.isSameSku() ? 0 : 1)
                    .thenComparing(byDistance));
        } else {
            sorted.sort(byDistance);
        }
        return fill(sorted, plan, remain);
    }

    /**
     * 第 3 层：按排序结果依次取 min(cap_i, 剩余量)，排满即停。
     * capacity 为 0 的库位直接跳过（硬过滤已保证至少能放 1 件，这里防御脏数据）。
     */
    private int fill(List<WmsPutawayCandidate> sorted, WmsPutawayPlan plan, int remain) {
        for (WmsPutawayCandidate candidate : sorted) {
            if (remain <= 0) {
                break;
            }
            int capacity = candidate.getCapacity();
            if (capacity <= 0) {
                continue;
            }
            int put = Math.min(capacity, remain);
            WmsWarehouseAreaLoca loca = candidate.getLoca();
            plan.addLine(new WmsPutawayPlanLine()
                    .setLocaId(loca.getId())
                    .setLocaCode(loca.getLocaCode())
                    .setAreaId(loca.getWarehouseAreaId())
                    .setCount(put)
                    .setCapacity(capacity)
                    .setOverflow(candidate.isOverflow())
                    .setSameSku(candidate.isSameSku()));
            remain -= put;
        }
        return remain;
    }

    // ------------------------------------------------------------------
    // 公共小工具
    // ------------------------------------------------------------------

    /** 解析生效策略：SKU 配置优先，为空取仓库默认（再为空取 ABC） */
    private WmsPutawayStrategyEnum resolveStrategy(WmsItemSku sku, Integer warehouseId) {
        if (oConvertUtils.isNotEmpty(sku.getPutawayStrategy())) {
            return WmsPutawayStrategyEnum.of(sku.getPutawayStrategy());
        }
        String defaultStrategy = strategyConfigService.getConfigValue(
                warehouseId, "putaway_default_strategy", WmsPutawayStrategyEnum.ABC.getCode());
        return WmsPutawayStrategyEnum.of(defaultStrategy);
    }

    /**
     * 商品分区推导。阈值全部取自 wms_strategy_config（ZONE 分组），
     * 单位按 SPU 上的 *_unit 换算到基准单位（mm / g / cm³）。
     */
    private WmsZoneTypeEnum deriveItemZone(WmsItemSku sku, WmsItemSpu spu, Integer warehouseId) {
        double price = sku.getPrice() == null ? 0D : sku.getPrice().doubleValue();
        double valuablePrice = strategyConfigService.getDoubleConfig(warehouseId, "zone_valuable_price", 5000D);
        int largeVolume = strategyConfigService.getIntConfig(warehouseId, "zone_large_volume", 125000);
        int largeWeight = strategyConfigService.getIntConfig(warehouseId, "zone_large_weight", 25000);
        int largeSide = strategyConfigService.getIntConfig(warehouseId, "zone_large_side", 1200);
        int smallVolume = strategyConfigService.getIntConfig(warehouseId, "zone_small_volume", 1000);

        int volume = convertVolume(sku.getProductVolume(), spu);
        int weight = convertWeight(sku.getProductWeight(), spu);
        int maxSide = Math.max(
                Math.max(convertLength(sku.getProductLength(), spu), convertLength(sku.getProductWidth(), spu)),
                convertLength(sku.getProductHeight(), spu));
        boolean valuable = spu != null && spu.getIsValuable() != null && spu.getIsValuable() == 1;

        return WmsZoneTypeEnum.derive(valuable, price, valuablePrice,
                volume, weight, maxSide, largeVolume, largeWeight, largeSide, smallVolume);
    }

    /**
     * 商品等级：优先取 wms_abc_class_result 的当前生效等级，
     * 其次 SPU 上的兜底等级，都没有按 C（放最不占地方的库位，宁松不紧）。
     */
    private String resolveItemLevel(WmsItemSku sku, WmsItemSpu spu, Integer warehouseId) {
        // 这里不引入 ABC 计算模块的依赖，直接按生效结果表取；取不到再兜底到 SPU
        String level = skuMapper.selectCurrentAbcClass(warehouseId, sku.getId());
        if (oConvertUtils.isNotEmpty(level)) {
            return level;
        }
        if (spu != null && oConvertUtils.isNotEmpty(spu.getAbcClass())) {
            return spu.getAbcClass();
        }
        return "C";
    }

    /** 等级差距：0 同级、正数表示库位等级比商品低（A 商品 vs B 库位 = 1） */
    private int levelGap(String locaClass, String itemClass) {
        int loca = abcRank(locaClass);
        int item = abcRank(itemClass);
        // 库位没有等级数据时按「同级」处理，避免整仓没算过等级就全都上不了架
        if (loca < 0) {
            return 0;
        }
        return loca - item;
    }

    /** A=0 B=1 C=2，未知返回 -1 */
    private int abcRank(String abcClass) {
        if (abcClass == null) {
            return -1;
        }
        switch (abcClass.trim().toUpperCase()) {
            case "A":
                return 0;
            case "B":
                return 1;
            case "C":
                return 2;
            default:
                return -1;
        }
    }

    /** 分区匹配：商品分区允许的兜底分区里包含库区分区即可（方案 3.5.2 兜底表） */
    private boolean matchesZone(WmsZoneTypeEnum itemZone, WmsZoneTypeEnum areaZone) {
        for (WmsZoneTypeEnum allowed : itemZone.fallbacks()) {
            if (allowed == areaZone) {
                return true;
            }
        }
        return false;
    }

    /** 存储条件以 SPU 为准，未设置按常温 */
    private String resolveStorageCondition(WmsItemSpu spu) {
        if (spu != null && oConvertUtils.isNotEmpty(spu.getStorageCondition())) {
            return spu.getStorageCondition();
        }
        return DEFAULT_STORAGE_CONDITION;
    }

    /**
     * 库位的生效存储条件：库位单独配了用它，否则继承所属库区，都没有按常温。
     * 方案 3.5.2 的温区校验写在库区上，库位级是更细的覆盖。
     */
    private String resolveLocaStorageCondition(WmsWarehouseAreaLoca loca, WmsWarehouseArea area) {
        if (oConvertUtils.isNotEmpty(loca.getStorageCondition())) {
            return loca.getStorageCondition();
        }
        if (area != null && oConvertUtils.isNotEmpty(area.getStorageCondition())) {
            return area.getStorageCondition();
        }
        return DEFAULT_STORAGE_CONDITION;
    }

    // --- 单位换算：入库比较前统一到 mm / g / cm³ ---

    private int convertLength(Integer value, WmsItemSpu spu) {
        if (value == null) {
            return 0;
        }
        String unit = spu == null || spu.getLengthUnit() == null ? "mm" : spu.getLengthUnit().trim().toLowerCase();
        switch (unit) {
            case "cm":
                return value * 10;
            case "dm":
                return value * 100;
            case "m":
                return value * 1000;
            default:
                return value;
        }
    }

    private int convertWeight(Integer value, WmsItemSpu spu) {
        if (value == null) {
            return 0;
        }
        String unit = spu == null || spu.getWeightUnit() == null ? "g" : spu.getWeightUnit().trim().toLowerCase();
        switch (unit) {
            case "mg":
                return value / 1000;
            case "kg":
                return value * 1000;
            default:
                return value;
        }
    }

    private int convertVolume(Integer value, WmsItemSpu spu) {
        if (value == null) {
            return 0;
        }
        String unit = spu == null || spu.getVolumeUnit() == null ? "cm³" : spu.getVolumeUnit().trim().toLowerCase();
        switch (unit) {
            case "dm³":
            case "dm3":
            case "l":
                return value * 1000;
            case "m³":
            case "m3":
                return value * 1000000;
            default:
                return value;
        }
    }

    /** 单件体积（cm³）。商品没维护体积时用长宽高估算，仍为 0 表示不占体积 */
    private int unitVolume(WmsItemSku sku) {
        int volume = countOf(sku.getProductVolume());
        if (volume > 0) {
            return volume;
        }
        int l = countOf(sku.getProductLength());
        int w = countOf(sku.getProductWidth());
        int h = countOf(sku.getProductHeight());
        return l > 0 && w > 0 && h > 0 ? l * w * h : 0;
    }

    private int unitWeight(WmsItemSku sku) {
        return countOf(sku.getProductWeight());
    }

    private int countOf(Integer value) {
        return value == null ? 0 : value;
    }

    private WmsWarehouseArea loadArea(Integer areaId) {
        return areaId == null ? null : areaMapper.selectById(areaId);
    }

    private List<WmsInventory> loadOccupied(Integer locaId) {
        if (locaId == null) {
            return new ArrayList<>();
        }
        return inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseAreaLocaId, locaId)
                .gt(WmsInventory::getTotalCount, 0));
    }

    /** 批量取多个库位的占用库存，按库位分组 */
    private Map<Integer, List<WmsInventory>> loadOccupiedByLocas(List<Integer> locaIds) {
        Map<Integer, List<WmsInventory>> map = new HashMap<>();
        if (locaIds == null || locaIds.isEmpty()) {
            return map;
        }
        List<WmsInventory> rows = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .in(WmsInventory::getWarehouseAreaLocaId, locaIds)
                .gt(WmsInventory::getTotalCount, 0));
        for (WmsInventory row : rows) {
            map.computeIfAbsent(row.getWarehouseAreaLocaId(), k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    @Override
    public WmsWarehouseAreaLoca checkForcedLoca(WmsPutawayRequest request, Integer locaId) {
        if (request == null || request.getWarehouseId() == null) {
            throw new JeecgBootException("请指定仓库");
        }
        if (locaId == null) {
            throw new JeecgBootException("请指定目标库位");
        }
        WmsWarehouseAreaLoca loca = locaMapper.selectById(locaId);
        if (loca == null) {
            throw new JeecgBootException("目标库位不存在");
        }
        // 跨仓上架在物理上就不成立：货在这个仓，库位在另一个仓
        if (!request.getWarehouseId().equals(loca.getWarehouseId())) {
            throw new JeecgBootException("库位[" + loca.getLocaCode() + "]不属于本仓库，不能跨仓上架");
        }
        if (loca.getLocaStatus() != null && loca.getLocaStatus() == LOCA_STATUS_DISABLED) {
            throw new JeecgBootException("库位[" + loca.getLocaCode() + "]已禁用，不能上架");
        }
        if (loca.getLocaStatus() != null && loca.getLocaStatus() == LOCA_STATUS_STOCKTAKING) {
            throw new JeecgBootException("库位[" + loca.getLocaCode() + "]正在盘点锁定，不能上架");
        }
        return loca;
    }

    // ------------------------------------------------------------------
    // 人工指定库位的下拉选项
    // ------------------------------------------------------------------

    @Override
    public List<WmsPutawayLocaOption> listLocaOptions(WmsPutawayRequest request) {
        if (request == null || request.getWarehouseId() == null) {
            throw new JeecgBootException("请指定仓库");
        }
        Integer warehouseId = request.getWarehouseId();
        // 下拉不按分区/温区过滤（强制指定就是让人越过分区判断），但状态上排除禁用与盘点锁定，
        // 这两种库位任何情况下都不该被选中，列出来只会让作业员白填一遍原因。
        List<WmsWarehouseArea> areas = areaMapper.selectList(new LambdaQueryWrapper<WmsWarehouseArea>()
                .eq(WmsWarehouseArea::getWarehouseId, warehouseId)
                .orderByAsc(WmsWarehouseArea::getId));
        if (areas.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Integer, WmsWarehouseArea> areaMap = areas.stream()
                .collect(Collectors.toMap(WmsWarehouseArea::getId, a -> a, (a, b) -> a));
        List<WmsWarehouseAreaLoca> locas = locaMapper.selectList(new LambdaQueryWrapper<WmsWarehouseAreaLoca>()
                .eq(WmsWarehouseAreaLoca::getWarehouseId, warehouseId)
                .in(WmsWarehouseAreaLoca::getWarehouseAreaId, areaMap.keySet())
                .notIn(WmsWarehouseAreaLoca::getLocaStatus, LOCA_STATUS_STOCKTAKING, LOCA_STATUS_DISABLED)
                .orderByAsc(WmsWarehouseAreaLoca::getWarehouseAreaId)
                .orderByAsc(WmsWarehouseAreaLoca::getLocaCode));
        if (locas.isEmpty()) {
            return new ArrayList<>();
        }

        WmsItemSku sku = request.getSkuId() == null ? null : skuMapper.selectById(request.getSkuId());
        WmsItemSpu spu = sku == null || sku.getSpuId() == null ? null : spuMapper.selectById(sku.getSpuId());
        WmsZoneTypeEnum itemZone = sku == null ? null : deriveItemZone(sku, spu, warehouseId);
        Map<Integer, List<WmsInventory>> occupiedMap = loadOccupiedByLocas(
                locas.stream().map(WmsWarehouseAreaLoca::getId).collect(Collectors.toList()));

        List<WmsPutawayLocaOption> options = new ArrayList<>();
        for (WmsWarehouseAreaLoca loca : locas) {
            WmsWarehouseArea area = areaMap.get(loca.getWarehouseAreaId());
            String reason = "";
            if (sku != null) {
                try {
                    // 与推荐用同一套断言，只是这里不排除，改为把原因挂在选项上
                    assertLocaAllowed(request, sku, spu, itemZone, loca, area,
                            occupiedMap.getOrDefault(loca.getId(), new ArrayList<>()), request.getCount());
                } catch (JeecgBootException e) {
                    reason = e.getMessage();
                }
            }
            options.add(new WmsPutawayLocaOption()
                    .setLocaId(loca.getId())
                    .setLocaCode(loca.getLocaCode())
                    .setGroup(area == null ? "未分配库区" : area.getName())
                    .setAreaId(loca.getWarehouseAreaId())
                    .setAreaCategory(area == null ? null : area.getCategory())
                    .setZoneType(area == null ? null : area.getZoneType())
                    .setStorageCondition(resolveLocaStorageCondition(loca, area))
                    .setLocaStatus(loca.getLocaStatus())
                    .setRemainVolume(loca.getLocalVolume() == null || loca.getLocalVolume() <= 0
                            ? null : Math.max(0, loca.getLocalVolume() - countOf(loca.getUsedVolume())))
                    .setRemainWeight(loca.getLocalBearingCapacity() == null || loca.getLocalBearingCapacity() <= 0
                            ? null : Math.max(0, loca.getLocalBearingCapacity() - countOf(loca.getUsedWeight())))
                    .setFixedSkuId(loca.getFixedSkuId())
                    .setAllowed(reason.isEmpty())
                    .setRejectReason(reason.isEmpty() ? null : reason));
        }
        return options;
    }

    // ------------------------------------------------------------------
    // 被淘汰原因的归并
    // ------------------------------------------------------------------

    /** 把断言消息里的「库位[编码]」抹成「库位」，同一类原因才能归并成一条 */
    private String reasonOf(String message, String locaCode) {
        if (message == null) {
            return "未知原因";
        }
        if (locaCode == null) {
            return message;
        }
        return message.replace("库位[" + locaCode + "]", "库位");
    }

    /**
     * 挑出最能说明问题的原因：命中次数最多的那条优先，
     * 前面拼上「本仓 N 个库位均不满足」。同类原因是按条数归并的，第一条就是最常见的。
     */
    private String summarizeRejected(LinkedHashMap<String, Integer> rejected, WmsZoneTypeEnum itemZone) {
        if (rejected.isEmpty()) {
            return "本仓没有可用于上架的库位";
        }
        Map.Entry<String, Integer> top = rejected.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .orElse(null);
        int total = rejected.values().stream().mapToInt(Integer::intValue).sum();
        String reason = top == null ? "未知原因" : top.getKey();
        return String.format("商品分区为 %s，本仓 %d 个库位均不满足，主要原因：%s",
                itemZone.getDesc(), total, reason);
    }

    private void validateRequest(WmsPutawayRequest request) {
        if (request == null) {
            throw new JeecgBootException("上架推荐参数不能为空");
        }
        if (request.getWarehouseId() == null) {
            throw new JeecgBootException("请指定仓库");
        }
        if (request.getSkuId() == null) {
            throw new JeecgBootException("请指定商品SKU");
        }
        if (request.getCount() <= 0) {
            throw new JeecgBootException("上架数量必须大于0");
        }
    }
}
