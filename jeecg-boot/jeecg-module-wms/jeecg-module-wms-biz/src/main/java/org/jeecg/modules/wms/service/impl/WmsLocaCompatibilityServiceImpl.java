package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsItemSpu;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.mapper.WmsItemSkuMapper;
import org.jeecg.modules.wms.mapper.WmsItemSpuMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.service.IWmsLocaCompatibilityService;
import org.jeecg.modules.wms.util.WmsUnitConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description: 库位兼容性校验
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Slf4j
@Service
public class WmsLocaCompatibilityServiceImpl implements IWmsLocaCompatibilityService {

    /** 库位状态：盘点锁定 */
    private static final int LOCA_STATUS_STOCKTAKING = 3;
    /** 库位状态：禁用 */
    private static final int LOCA_STATUS_DISABLED = 4;
    /** 默认存储条件 */
    private static final String DEFAULT_STORAGE_CONDITION = "NORMAL";

    @Autowired
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Autowired
    private WmsItemSkuMapper skuMapper;

    @Autowired
    private WmsItemSpuMapper spuMapper;

    @Autowired
    private WmsInventoryMapper inventoryMapper;

    @Override
    public WmsWarehouseAreaLoca checkTarget(Integer targetLocaId, Long skuId, String batchNumber, int count) {
        if (targetLocaId == null) {
            throw new JeecgBootException("请指定目标库位");
        }
        if (skuId == null) {
            throw new JeecgBootException("请指定商品SKU");
        }
        if (count <= 0) {
            throw new JeecgBootException("移库数量必须大于0");
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
            throw new JeecgBootException("目标库位[" + loca.getLocaCode() + "]正在盘点锁定，不能移入");
        }

        // 固定库位只允许存放绑定的 SKU
        if (loca.getFixedSkuId() != null && !loca.getFixedSkuId().equals(skuId)) {
            throw new JeecgBootException("目标库位[" + loca.getLocaCode() + "]是固定库位，只能存放绑定的SKU");
        }

        WmsItemSku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new JeecgBootException("商品SKU不存在");
        }

        // ② 存储温区匹配（以 SPU 的存储条件为准，未设置按常温）
        WmsItemSpu spu = sku.getSpuId() == null ? null : spuMapper.selectById(sku.getSpuId());
        String required = DEFAULT_STORAGE_CONDITION;
        if (spu != null && oConvertUtils.isNotEmpty(spu.getStorageCondition())) {
            required = spu.getStorageCondition();
        }
        String targetCondition = oConvertUtils.isEmpty(loca.getStorageCondition())
                ? DEFAULT_STORAGE_CONDITION : loca.getStorageCondition();
        if (!required.equals(targetCondition)) {
            throw new JeecgBootException(String.format(
                    "目标库位[%s]存储条件为 %s，与商品要求的 %s 不匹配",
                    loca.getLocaCode(), targetCondition, required));
        }

        // ③ 混放策略
        String batch = batchNumber == null ? "" : batchNumber;
        List<WmsInventory> occupied = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseAreaLocaId, targetLocaId)
                .gt(WmsInventory::getTotalCount, 0));
        for (WmsInventory row : occupied) {
            if (row.getSkuId() != null && !row.getSkuId().equals(skuId)
                    && (loca.getAllowMixSku() == null || loca.getAllowMixSku() != 1)) {
                throw new JeecgBootException("目标库位[" + loca.getLocaCode() + "]不允许混放SKU");
            }
            String rowBatch = row.getBatchNumber() == null ? "" : row.getBatchNumber();
            if (!rowBatch.equals(batch)
                    && (loca.getAllowMixBatch() == null || loca.getAllowMixBatch() != 1)) {
                throw new JeecgBootException("目标库位[" + loca.getLocaCode() + "]不允许混放批次");
            }
        }

        // ④ 剩余容积与剩余承重（未配置时跳过）。商品侧按 SPU 单位换算到 cm³ / g 再比
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
        try {
            checkTarget(targetLocaId, skuId, batchNumber, count);
            return true;
        } catch (JeecgBootException e) {
            log.debug("库位 {} 不满足兼容性：{}", targetLocaId, e.getMessage());
            return false;
        }
    }
}
