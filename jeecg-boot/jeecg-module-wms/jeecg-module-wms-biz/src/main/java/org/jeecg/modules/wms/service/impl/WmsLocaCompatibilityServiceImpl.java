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
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.mapper.WmsItemSkuMapper;
import org.jeecg.modules.wms.mapper.WmsItemSpuMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.service.IWmsLocaCompatibilityService;
import org.jeecg.modules.wms.service.IWmsStrategyConfigService;
import org.jeecg.modules.wms.util.WmsUnitConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @Description: 统一落库兼容性与校验服务实现（上架与移库共用）
 * 核心规则：
 * 1. 目标库位未禁用、未盘点锁定；
 * 2. 质量状态与库区类别匹配（良品只能进 STORAGE/PICKING，不良品只能进 RETURN）；
 * 3. 存储温区与商品要求匹配；
 * 4. 固定库位匹配（已绑定的只能放绑定的 SKU）；
 * 5. 一库位一 SKU 一批次（STORAGE/PICKING 严禁混 SKU、严禁混批次；RETURN 允许混放）；
 * 6. 物理容积与承重充足（未配置时跳过）。
 * 不再读写 max_count 与 SPU 规划容量！
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Slf4j
@Service
public class WmsLocaCompatibilityServiceImpl implements IWmsLocaCompatibilityService {

    /** 库位状态：空闲 */
    private static final int LOCA_STATUS_EMPTY = 1;
    /** 库位状态：占用 */
    private static final int LOCA_STATUS_OCCUPIED = 2;
    /** 库位状态：盘点锁定 */
    private static final int LOCA_STATUS_STOCKTAKING = 3;
    /** 库位状态：禁用 */
    private static final int LOCA_STATUS_DISABLED = 4;

    /** 库区类型：存储区 */
    private static final String AREA_STORAGE = "STORAGE";
    /** 库区类型：拣货区 */
    private static final String AREA_PICKING = "PICKING";
    /** 库区类型：待检区 */
    private static final String AREA_QC = "QC";
    /** 库区类型：退货不良区 */
    private static final String AREA_RETURN = "RETURN";

    /** 默认存储条件 */
    private static final String DEFAULT_STORAGE_CONDITION = "NORMAL";
    /** 质量状态：良品 */
    private static final int QUALITY_GOOD = 1;

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

    @Autowired(required = false)
    private IWmsStrategyConfigService strategyConfigService;

    @Override
    public WmsWarehouseAreaLoca checkTarget(Integer targetLocaId, Long skuId, String batchNumber, int count) {
        return checkInbound(targetLocaId, skuId, batchNumber, count, QUALITY_GOOD);
    }

    @Override
    public WmsWarehouseAreaLoca checkInbound(Integer targetLocaId, Long skuId, String batchNumber, int count, Integer qualityStatus) {
        if (targetLocaId == null) {
            throw new JeecgBootException("请指定目标库位");
        }
        if (skuId == null) {
            throw new JeecgBootException("请指定商品SKU");
        }
        if (count <= 0) {
            throw new JeecgBootException("操作数量必须大于0");
        }
        WmsWarehouseAreaLoca loca = locaMapper.selectById(targetLocaId);
        if (loca == null) {
            throw new JeecgBootException("目标库位不存在");
        }

        // ① 未禁用、未盘点锁定
        if (loca.getLocaStatus() != null && loca.getLocaStatus() == LOCA_STATUS_DISABLED) {
            throw new JeecgBootException("目标库位[" + loca.getLocaCode() + "]已禁用");
        }
        if (loca.getLocaStatus() != null && loca.getLocaStatus() == LOCA_STATUS_STOCKTAKING) {
            throw new JeecgBootException("目标库位[" + loca.getLocaCode() + "]正在盘点锁定，不能操作");
        }

        // ② 质量状态与库区类别匹配
        WmsWarehouseArea area = loca.getWarehouseAreaId() != null ? areaMapper.selectById(loca.getWarehouseAreaId()) : null;
        String category = area != null && area.getCategory() != null ? area.getCategory() : "";
        int qStatus = qualityStatus == null ? QUALITY_GOOD : qualityStatus;
        if (qStatus == QUALITY_GOOD) {
            if (!AREA_STORAGE.equals(category) && !AREA_PICKING.equals(category)) {
                throw new JeecgBootException(String.format(
                        "良品只能上架/移入到存储区或拣货区，目标库位[%s]所在库区类型为 %s",
                        loca.getLocaCode(), category));
            }
        } else {
            if (!AREA_RETURN.equals(category) && !AREA_QC.equals(category)) {
                throw new JeecgBootException(String.format(
                        "不良品只能上架/移入到退货不良区(RETURN)，目标库位[%s]所在库区类型为 %s",
                        loca.getLocaCode(), category));
            }
        }

        WmsItemSku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new JeecgBootException("商品SKU不存在");
        }
        WmsItemSpu spu = sku.getSpuId() == null ? null : spuMapper.selectById(sku.getSpuId());

        // ③ 存储温区匹配（以 SPU 的存储条件为准，未设置按常温）
        String required = resolveStorageCondition(spu);
        String targetCondition = resolveLocaStorageCondition(loca, area);
        if (!required.equals(targetCondition)) {
            throw new JeecgBootException(String.format(
                    "目标库位[%s]存储条件为 %s，与商品要求的 %s 不匹配",
                    loca.getLocaCode(), targetCondition, required));
        }

        // ④ 固定库位只允许存放绑定的 SKU
        if (loca.getFixedSkuId() != null && !loca.getFixedSkuId().equals(skuId)) {
            throw new JeecgBootException("目标库位[" + loca.getLocaCode() + "]是固定库位，只能存放绑定的SKU");
        }

        // ⑤ 混放策略：一库位一 SKU 一批次（RETURN 退货不良区允许混放）
        String batch = batchNumber == null ? "" : batchNumber;
        List<WmsInventory> occupied = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseAreaLocaId, targetLocaId)
                .gt(WmsInventory::getTotalCount, 0));
        if (!AREA_RETURN.equals(category)) {
            for (WmsInventory row : occupied) {
                if (row.getTotalCount() == null || row.getTotalCount() <= 0) {
                    continue;
                }
                if (row.getSkuId() != null && !row.getSkuId().equals(skuId)) {
                    throw new JeecgBootException("目标库位[" + loca.getLocaCode() + "]不允许混放SKU");
                }
                String rowBatch = row.getBatchNumber() == null ? "" : row.getBatchNumber();
                if (!rowBatch.equals(batch)) {
                    throw new JeecgBootException("目标库位[" + loca.getLocaCode() + "]不允许混放批次");
                }
            }
        }

        // ⑦ 剩余容积与剩余承重（未配置时跳过，配置了且超限才拦截）
        int needVolume = WmsUnitConverter.skuUnitVolumeCm3(sku, spu) * count;
        int needWeight = WmsUnitConverter.skuUnitWeightG(sku, spu) * count;
        int localVolume = loca.getLocalVolume() == null ? 0 : loca.getLocalVolume();
        int localBearing = loca.getLocalBearingCapacity() == null ? 0 : loca.getLocalBearingCapacity();
        int usedVolume = loca.getUsedVolume() == null ? 0 : loca.getUsedVolume();
        int usedWeight = loca.getUsedWeight() == null ? 0 : loca.getUsedWeight();
        if (localVolume > 0 && usedVolume + needVolume > localVolume) {
            throw new JeecgBootException(String.format(
                    "目标库位[%s]容积不足：剩余 %d cm³，本次需要 %d cm³",
                    loca.getLocaCode(), localVolume - usedVolume, needVolume));
        }
        if (localBearing > 0 && usedWeight + needWeight > localBearing) {
            throw new JeecgBootException(String.format(
                    "目标库位[%s]承重不足：剩余 %d g，本次需要 %d g",
                    loca.getLocaCode(), localBearing - usedWeight, needWeight));
        }
        return loca;
    }

    @Override
    public boolean isCompatible(Integer targetLocaId, Long skuId, String batchNumber, int count) {
        return isCompatible(targetLocaId, skuId, batchNumber, count, QUALITY_GOOD);
    }

    @Override
    public boolean isCompatible(Integer targetLocaId, Long skuId, String batchNumber, int count, Integer qualityStatus) {
        try {
            checkInbound(targetLocaId, skuId, batchNumber, count, qualityStatus);
            return true;
        } catch (JeecgBootException e) {
            log.debug("库位 {} 不满足兼容性：{}", targetLocaId, e.getMessage());
            return false;
        }
    }

    @Override
    public void refreshLocaOccupancy(Integer locaId) {
        if (locaId == null) {
            return;
        }
        WmsWarehouseAreaLoca current = locaMapper.selectById(locaId);
        if (current == null) {
            return;
        }

        List<WmsInventory> rows = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseAreaLocaId, locaId)
                .eq(WmsInventory::getIsDel, 1));
        int total = rows.stream().mapToInt(r -> r.getTotalCount() == null ? 0 : Math.max(0, r.getTotalCount())).sum();
        int usedVol = rows.stream().mapToInt(r -> r.getUsedVolume() == null ? 0 : Math.max(0, r.getUsedVolume())).sum();
        int usedWt = rows.stream().mapToInt(r -> r.getUsedWeight() == null ? 0 : Math.max(0, r.getUsedWeight())).sum();

        WmsWarehouseAreaLoca update = new WmsWarehouseAreaLoca().setId(locaId)
                .setUsedVolume(usedVol)
                .setUsedWeight(usedWt)
                .setUpdateTime(new Date());

        // 盘点锁定(3)与禁用(4)不自动覆盖
        if (current.getLocaStatus() != null && (current.getLocaStatus() == LOCA_STATUS_STOCKTAKING || current.getLocaStatus() == LOCA_STATUS_DISABLED)) {
            // 保持
        } else {
            update.setLocaStatus(total > 0 ? LOCA_STATUS_OCCUPIED : LOCA_STATUS_EMPTY);
        }

        locaMapper.updateById(update);
    }

    private String resolveStorageCondition(WmsItemSpu spu) {
        if (spu != null && oConvertUtils.isNotEmpty(spu.getStorageCondition())) {
            return normalizeStorageCondition(spu.getStorageCondition());
        }
        return DEFAULT_STORAGE_CONDITION;
    }

    private String resolveLocaStorageCondition(WmsWarehouseAreaLoca loca, WmsWarehouseArea area) {
        if (oConvertUtils.isNotEmpty(loca.getStorageCondition())) {
            return normalizeStorageCondition(loca.getStorageCondition());
        }
        if (area != null && oConvertUtils.isNotEmpty(area.getStorageCondition())) {
            return normalizeStorageCondition(area.getStorageCondition());
        }
        return DEFAULT_STORAGE_CONDITION;
    }

    private String normalizeStorageCondition(String cond) {
        if (cond == null) return DEFAULT_STORAGE_CONDITION;
        String trimmed = cond.trim();
        if ("常温".equals(trimmed) || "NORMAL".equalsIgnoreCase(trimmed)) return "NORMAL";
        if ("阴凉".equals(trimmed) || "COOL".equalsIgnoreCase(trimmed)) return "COOL";
        if ("冷藏".equals(trimmed) || "COLD".equalsIgnoreCase(trimmed)) return "COLD";
        if ("冷冻".equals(trimmed) || "FROZEN".equalsIgnoreCase(trimmed)) return "FROZEN";
        if ("防静电".equals(trimmed) || "ESD".equalsIgnoreCase(trimmed)) return "ESD";
        if ("危化".equals(trimmed) || "HAZARD".equalsIgnoreCase(trimmed)) return "HAZARD";
        return trimmed.toUpperCase();
    }
}
