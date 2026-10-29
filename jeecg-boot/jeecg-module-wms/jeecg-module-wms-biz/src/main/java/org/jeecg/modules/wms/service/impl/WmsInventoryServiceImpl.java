package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsInventoryLog;
import org.jeecg.modules.wms.enums.WmsInventoryBizTypeEnum;
import org.jeecg.modules.wms.mapper.WmsInventoryLogMapper;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.vo.WmsInventoryDimension;
import org.jeecg.modules.wms.vo.WmsInventoryPage;
import org.jeecg.modules.wms.vo.WmsInventoryQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * @Description: 多维实时库存
 *
 * 数量恒等式：available = total - locked - frozen。
 * 每次变动先按唯一键对库存行加行锁（SELECT ... FOR UPDATE），再按恒等式重算四个数量，
 * 避免并发出库 / 移库出现超卖或负库存。
 * 数量归零的库存行保留（唯一键已被占用），下次入库直接复用，不回删。
 *
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Slf4j
@Service
public class WmsInventoryServiceImpl extends ServiceImpl<WmsInventoryMapper, WmsInventory> implements IWmsInventoryService {

    /** 库存行未删除 */
    private static final int NOT_DELETED = 1;
    /** 库存行已删除（人工删除后又被复用的情况） */
    private static final int DELETED = 2;
    /** 质量状态：良品 */
    private static final int QUALITY_GOOD = 1;

    @Autowired
    private WmsInventoryLogMapper inventoryLogMapper;

    @Override
    public Long sumTotalCount(Integer warehouseId, Long skuId) {
        return baseMapper.sumTotalCount(warehouseId, skuId);
    }

    @Override
    public IPage<WmsInventoryPage> queryInventoryPage(WmsInventoryQuery query, Integer pageNo, Integer pageSize) {
        Page<WmsInventoryPage> page = new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize);
        return baseMapper.selectInventoryPage(page, query == null ? new WmsInventoryQuery() : query);
    }

    @Override
    public WmsInventory getByDimension(WmsInventoryDimension dimension) {
        if (dimension == null || dimension.getWarehouseAreaLocaId() == null || dimension.getSkuId() == null) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseAreaLocaId, dimension.getWarehouseAreaLocaId())
                .eq(WmsInventory::getSkuId, dimension.getSkuId())
                .eq(WmsInventory::getBatchNumber, dimension.getBatchNumberOrDefault())
                .eq(WmsInventory::getConsignorId, dimension.getConsignorIdOrDefault())
                .eq(WmsInventory::getQualityStatus, dimension.getQualityStatusOrDefault())
                .last("LIMIT 1"), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsInventory getByDimensionForUpdate(WmsInventoryDimension dimension) {
        checkDimension(dimension);
        return baseMapper.selectByDimensionForUpdate(dimension);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsInventory getOrCreate(WmsInventoryDimension dimension) {
        checkDimension(dimension);
        WmsInventory inventory = baseMapper.selectByDimensionForUpdate(dimension);
        if (inventory != null) {
            return inventory;
        }
        WmsInventory created = new WmsInventory()
                .setWarehouseId(dimension.getWarehouseId())
                .setWarehouseAreaId(dimension.getWarehouseAreaId())
                .setWarehouseAreaLocaId(dimension.getWarehouseAreaLocaId())
                .setConsignorId(dimension.getConsignorIdOrDefault())
                .setSkuId(dimension.getSkuId())
                .setBatchNumber(dimension.getBatchNumberOrDefault())
                .setProdDate(dimension.getProdDate())
                .setExpireDate(dimension.getExpireDate())
                .setQualityStatus(dimension.getQualityStatusOrDefault())
                .setTotalCount(0)
                .setAvailableCount(0)
                .setLockedCount(0)
                .setFrozenCount(0)
                .setUsedVolume(0)
                .setUsedWeight(0)
                .setVersion(0)
                .setIsDel(NOT_DELETED);
        baseMapper.insert(created);
        // 插入后再取一次行锁，保证同一事务内后续变动串行
        WmsInventory locked = baseMapper.selectByDimensionForUpdate(dimension);
        return locked == null ? created : locked;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsInventory applyChange(WmsInventoryDimension dimension, int totalDelta, int lockedDelta, int frozenDelta,
                                    WmsInventoryBizTypeEnum bizType, String bizNo, String remark) {
        checkDimension(dimension);
        if (bizType == null) {
            throw new JeecgBootException("库存变动必须指定业务类型");
        }
        if (totalDelta == 0 && lockedDelta == 0 && frozenDelta == 0) {
            throw new JeecgBootException("库存变动数量不能全为0");
        }
        WmsInventory inventory = getOrCreate(dimension);
        return doChange(inventory, dimension, totalDelta, lockedDelta, frozenDelta, bizType, bizNo, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsInventory adjustToTotal(WmsInventoryDimension dimension, int targetTotal,
                                      WmsInventoryBizTypeEnum bizType, String bizNo, String remark) {
        checkDimension(dimension);
        if (targetTotal < 0) {
            throw new JeecgBootException("调整后的库存数量不能为负数");
        }
        if (bizType == null) {
            throw new JeecgBootException("库存调整必须指定业务类型");
        }
        WmsInventory inventory = getOrCreate(dimension);
        int currentTotal = inventory.getTotalCount() == null ? 0 : inventory.getTotalCount();
        int delta = targetTotal - currentTotal;
        if (delta == 0) {
            return inventory;
        }
        return doChange(inventory, dimension, delta, 0, 0, bizType, bizNo, remark);
    }

    /**
     * 按恒等式重算四个数量并落库 + 记流水。
     * available 不由调用方给，始终 = total - locked - frozen，保证恒等式恒成立。
     */
    private WmsInventory doChange(WmsInventory inventory, WmsInventoryDimension dimension,
                                  int totalDelta, int lockedDelta, int frozenDelta,
                                  WmsInventoryBizTypeEnum bizType, String bizNo, String remark) {
        int beforeTotal = inventory.getTotalCount() == null ? 0 : inventory.getTotalCount();
        int beforeLocked = inventory.getLockedCount() == null ? 0 : inventory.getLockedCount();
        int beforeFrozen = inventory.getFrozenCount() == null ? 0 : inventory.getFrozenCount();
        int beforeAvailable = inventory.getAvailableCount() == null ? 0 : inventory.getAvailableCount();

        int afterTotal = beforeTotal + totalDelta;
        int afterLocked = beforeLocked + lockedDelta;
        int afterFrozen = beforeFrozen + frozenDelta;
        if (afterTotal < 0) {
            throw new JeecgBootException("库存不足：当前物理库存 " + beforeTotal + "，本次需扣减 " + (-totalDelta));
        }
        if (afterLocked < 0) {
            throw new JeecgBootException("锁定库存不足：当前锁定 " + beforeLocked + "，本次需解锁 " + (-lockedDelta));
        }
        if (afterFrozen < 0) {
            throw new JeecgBootException("冻结库存不足：当前冻结 " + beforeFrozen + "，本次需解冻 " + (-frozenDelta));
        }
        int afterAvailable = afterTotal - afterLocked - afterFrozen;
        if (afterAvailable < 0) {
            throw new JeecgBootException(String.format(
                    "可用库存不足：总库存 %d，锁定 %d，冻结 %d，本次变动后可用为 %d",
                    afterTotal, afterLocked, afterFrozen, afterAvailable));
        }

        int updated = baseMapper.updateCounts(inventory.getId(), afterTotal, afterAvailable, afterLocked, afterFrozen, NOT_DELETED);
        if (updated == 0) {
            throw new JeecgBootException("库存更新失败，请重试");
        }

        inventory.setTotalCount(afterTotal)
                .setAvailableCount(afterAvailable)
                .setLockedCount(afterLocked)
                .setFrozenCount(afterFrozen)
                .setIsDel(NOT_DELETED);

        writeLog(inventory, dimension, bizType, bizNo, remark, totalDelta, beforeAvailable, afterAvailable);
        return inventory;
    }

    private void writeLog(WmsInventory inventory, WmsInventoryDimension dimension, WmsInventoryBizTypeEnum bizType,
                          String bizNo, String remark, int changeCount, int beforeAvailable, int afterAvailable) {
        WmsInventoryLog inventoryLog = new WmsInventoryLog()
                .setInventoryId(inventory.getId())
                .setBizType(bizType.getCode())
                .setBizNo(bizNo)
                .setWarehouseId(inventory.getWarehouseId() != null ? inventory.getWarehouseId() : dimension.getWarehouseId())
                .setWarehouseAreaId(inventory.getWarehouseAreaId() != null ? inventory.getWarehouseAreaId() : dimension.getWarehouseAreaId())
                .setWarehouseAreaLocaId(inventory.getWarehouseAreaLocaId())
                .setConsignorId(inventory.getConsignorId())
                .setSkuId(inventory.getSkuId())
                .setBatchNumber(inventory.getBatchNumber())
                .setChangeCount(changeCount)
                .setBeforeCount(beforeAvailable)
                .setAfterCount(afterAvailable)
                .setRemark(remark)
                .setCreateTime(new Date());
        inventoryLogMapper.insert(inventoryLog);
    }

    private void checkDimension(WmsInventoryDimension dimension) {
        if (dimension == null
                || dimension.getWarehouseAreaLocaId() == null
                || dimension.getSkuId() == null) {
            throw new JeecgBootException("库存维度不完整：库位与SKU不能为空");
        }
        if (dimension.getWarehouseId() == null || dimension.getWarehouseAreaId() == null) {
            throw new JeecgBootException("库存维度不完整：仓库与库区不能为空");
        }
        if (dimension.getQualityStatusOrDefault() < QUALITY_GOOD) {
            throw new JeecgBootException("质量状态不合法");
        }
    }
}
