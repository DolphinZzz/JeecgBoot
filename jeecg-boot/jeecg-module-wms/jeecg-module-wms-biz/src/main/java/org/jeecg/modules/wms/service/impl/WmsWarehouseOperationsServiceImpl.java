package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.entity.WmsWarehouseOperations;
import org.jeecg.modules.wms.entity.WmsWarehouseOperationsDetails;
import org.jeecg.modules.wms.enums.WmsInventoryBizTypeEnum;
import org.jeecg.modules.wms.enums.WmsOperationStatusEnum;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.mapper.WmsItemSkuMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseOperationsDetailsMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseOperationsMapper;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.service.IWmsLocaCompatibilityService;
import org.jeecg.modules.wms.service.IWmsWarehouseOperationsService;
import org.jeecg.modules.wms.vo.WmsInventoryDimension;
import org.jeecg.modules.wms.vo.WmsOperationDetailForm;
import org.jeecg.modules.wms.vo.WmsOperationForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @Description: 库内作业（移库移位 / 补货）
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
@Slf4j
@Service
public class WmsWarehouseOperationsServiceImpl extends ServiceImpl<WmsWarehouseOperationsMapper, WmsWarehouseOperations>
        implements IWmsWarehouseOperationsService {

    /** 作业子类：移库移位 */
    private static final int OP_TYPE_MOVE = 1;
    /** 作业子类：补货 */
    private static final int OP_TYPE_REPLENISH = 2;
    /** 作业来源：人工创建 */
    private static final int SOURCE_MANUAL = 1;
    /** 作业来源：系统补货 */
    private static final int SOURCE_SYSTEM_REPLENISH = 3;
    /** 库区类型：存储区 */
    private static final String AREA_STORAGE = "STORAGE";
    /** 库区类型：拣货区 */
    private static final String AREA_PICKING = "PICKING";
    /** 明细行 category：既有表结构用它区分来源/目标行，二期一行同时携带来源与目标，固定填 1 */
    private static final int DETAIL_SOURCE = 1;

    @Autowired
    private WmsWarehouseOperationsDetailsMapper detailsMapper;

    @Autowired
    private WmsInventoryMapper inventoryMapper;

    @Autowired
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Autowired
    private WmsWarehouseAreaMapper areaMapper;

    @Autowired
    private WmsItemSkuMapper skuMapper;

    @Autowired
    private IWmsInventoryService inventoryService;

    @Autowired
    private IWmsLocaCompatibilityService locaCompatibilityService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsWarehouseOperations createOperation(WmsOperationForm form) {
        if (form == null || form.getDetails() == null || form.getDetails().isEmpty()) {
            throw new JeecgBootException("请至少填写一行移库/补货明细");
        }
        Integer operationType = form.getOperationType();
        if (operationType == null || (operationType != OP_TYPE_MOVE && operationType != OP_TYPE_REPLENISH)) {
            throw new JeecgBootException("作业子类只能是 1移库移位 或 2补货");
        }
        if (form.getWarehouseId() == null) {
            throw new JeecgBootException("请指定仓库");
        }

        WmsWarehouseOperations operation = new WmsWarehouseOperations()
                .setJobCode(nextJobCode(operationType))
                .setWarehouseId(form.getWarehouseId())
                .setCategory(operationType)
                .setOperationType(operationType)
                .setSourceType(operationType == OP_TYPE_REPLENISH ? SOURCE_SYSTEM_REPLENISH : SOURCE_MANUAL)
                .setStatus(WmsOperationStatusEnum.PENDING.getCode())
                .setRemark(form.getRemark())
                .setCreateTime(new Date())
                .setIsDel(1);
        baseMapper.insert(operation);

        for (WmsOperationDetailForm line : form.getDetails()) {
            WmsWarehouseOperationsDetails detail = buildDetail(operation, line, operationType);
            detailsMapper.insert(detail);
        }
        return operation;
    }

    /**
     * 校验并落一行明细。一行 = 一次「某库位某批次移到另一库位」，
     * 移出/移入库位与数量都存进同一行（source_* / target_*），执行时以行为单位原子完成。
     */
    private WmsWarehouseOperationsDetails buildDetail(WmsWarehouseOperations operation, WmsOperationDetailForm line, int operationType) {
        if (line == null || line.getSkuId() == null) {
            throw new JeecgBootException("明细行的商品SKU不能为空");
        }
        if (line.getCount() == null || line.getCount() <= 0) {
            throw new JeecgBootException("明细行的数量必须大于0");
        }
        if (line.getSourceWarehouseAreaLocaId() == null) {
            throw new JeecgBootException("明细行的移出库位不能为空");
        }
        WmsWarehouseAreaLoca sourceLoca = locaMapper.selectById(line.getSourceWarehouseAreaLocaId());
        if (sourceLoca == null) {
            throw new JeecgBootException("移出库位不存在");
        }
        if (operationType == OP_TYPE_MOVE && line.getTargetWarehouseAreaLocaId() == null) {
            throw new JeecgBootException("移库作业必须指定移入库位");
        }

        String batch = line.getBatchNumber() == null ? "" : line.getBatchNumber();
        int qualityStatus = line.getQualityStatus() == null ? 1 : line.getQualityStatus();
        WmsInventoryDimension dim = new WmsInventoryDimension()
                .setWarehouseId(sourceLoca.getWarehouseId())
                .setWarehouseAreaId(sourceLoca.getWarehouseAreaId())
                .setWarehouseAreaLocaId(sourceLoca.getId())
                .setConsignorId(line.getConsignorId())
                .setSkuId(line.getSkuId())
                .setBatchNumber(batch)
                .setQualityStatus(qualityStatus);
        WmsInventory source = inventoryService.getByDimension(dim);
        int available = source == null || source.getAvailableCount() == null ? 0 : source.getAvailableCount();
        if (available < line.getCount()) {
            throw new JeecgBootException(String.format(
                    "库位[%s]的SKU可用库存不足：可用 %d，本次需要 %d",
                    sourceLoca.getLocaCode(), available, line.getCount()));
        }
        if (line.getTargetWarehouseAreaLocaId() != null) {
            locaCompatibilityService.checkTarget(line.getTargetWarehouseAreaLocaId(), line.getSkuId(), batch, line.getCount());
        }

        return new WmsWarehouseOperationsDetails()
                .setWarehouseOperationsId(operation.getId())
                .setSkuId(line.getSkuId())
                .setBatchNumber(batch)
                .setConsignorId(line.getConsignorId() == null ? 0L : line.getConsignorId())
                .setQualityStatus(qualityStatus)
                .setSourceCount(line.getCount())
                .setTargetCount(line.getCount())
                .setSourceWarehouseAreaId(sourceLoca.getWarehouseAreaId())
                .setSourceWarehouseAreaLocaId(sourceLoca.getId())
                .setTargetWarehouseId(operation.getWarehouseId())
                .setTargetWarehouseAreaId(line.getTargetWarehouseAreaLocaId() == null ? null
                        : resolveAreaId(line.getTargetWarehouseAreaLocaId()))
                .setTargetWarehouseAreaLocaId(line.getTargetWarehouseAreaLocaId())
                .setCategory(DETAIL_SOURCE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> execute(Long id) {
        WmsWarehouseOperations operation = baseMapper.selectById(id);
        if (operation == null) {
            throw new JeecgBootException("作业单不存在");
        }
        if (operation.getStatus() == null || operation.getStatus() != WmsOperationStatusEnum.PENDING.getCode()) {
            throw new JeecgBootException("只有「未确认」的作业单可以执行，当前状态：" + operation.getStatus());
        }
        List<WmsWarehouseOperationsDetails> details = detailsMapper.selectList(
                new LambdaQueryWrapper<WmsWarehouseOperationsDetails>()
                        .eq(WmsWarehouseOperationsDetails::getWarehouseOperationsId, id)
                        .orderByAsc(WmsWarehouseOperationsDetails::getId));
        if (details.isEmpty()) {
            throw new JeecgBootException("作业单没有明细，无法执行");
        }

        WmsInventoryBizTypeEnum moveOutBiz = operation.getOperationType() != null && operation.getOperationType() == OP_TYPE_REPLENISH
                ? WmsInventoryBizTypeEnum.REPLENISH : WmsInventoryBizTypeEnum.MOVE_OUT;
        WmsInventoryBizTypeEnum moveInBiz = operation.getOperationType() != null && operation.getOperationType() == OP_TYPE_REPLENISH
                ? WmsInventoryBizTypeEnum.REPLENISH : WmsInventoryBizTypeEnum.MOVE_IN;

        long totalCount = 0;
        for (WmsWarehouseOperationsDetails detail : details) {
            int count = detail.getSourceCount() == null ? 0 : detail.getSourceCount();
            if (count <= 0) {
                continue;
            }
            if (detail.getTargetWarehouseAreaLocaId() == null) {
                throw new JeecgBootException("明细行未指定移入库位，无法执行");
            }
            // 执行前再校验一次目标库位，期间的盘点锁 / 禁用 / 容量变化都要拦住
            locaCompatibilityService.checkTarget(detail.getTargetWarehouseAreaLocaId(), detail.getSkuId(),
                    detail.getBatchNumber(), count);

            WmsInventoryDimension sourceDim = new WmsInventoryDimension()
                    .setWarehouseId(resolveWarehouseId(detail.getSourceWarehouseAreaLocaId()))
                    .setWarehouseAreaId(detail.getSourceWarehouseAreaId())
                    .setWarehouseAreaLocaId(detail.getSourceWarehouseAreaLocaId())
                    .setConsignorId(detail.getConsignorId())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setQualityStatus(detail.getQualityStatus());

            // 移出：先扣源库位
            inventoryService.applyChange(sourceDim, -count, 0, 0, moveOutBiz, operation.getJobCode(),
                    "移库移出到库位ID " + detail.getTargetWarehouseAreaLocaId());

            // 移入：目标库位同维度增加（生产/有效期沿用源库存行）
            WmsInventory target = inventoryService.getByDimension(new WmsInventoryDimension()
                    .setWarehouseAreaLocaId(detail.getTargetWarehouseAreaLocaId())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setConsignorId(detail.getConsignorId())
                    .setQualityStatus(detail.getQualityStatus()));
            WmsInventoryDimension targetDim = new WmsInventoryDimension()
                    .setWarehouseId(resolveWarehouseId(detail.getTargetWarehouseAreaLocaId()))
                    .setWarehouseAreaId(resolveAreaId(detail.getTargetWarehouseAreaLocaId()))
                    .setWarehouseAreaLocaId(detail.getTargetWarehouseAreaLocaId())
                    .setConsignorId(detail.getConsignorId())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setQualityStatus(detail.getQualityStatus())
                    .setProdDate(target == null ? null : target.getProdDate())
                    .setExpireDate(target == null ? null : target.getExpireDate());
            inventoryService.applyChange(targetDim, count, 0, 0, moveInBiz, operation.getJobCode(),
                    "移库移入，来源库位ID " + detail.getSourceWarehouseAreaLocaId());

            totalCount += count;
        }

        operation.setStatus(WmsOperationStatusEnum.EXECUTED.getCode()).setUpdateTime(new Date());
        baseMapper.updateById(operation);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobCode", operation.getJobCode());
        result.put("executedRows", details.size());
        result.put("movedCount", totalCount);
        result.put("message", "作业单[" + operation.getJobCode() + "]已执行，共移动 " + totalCount + " 件");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        WmsWarehouseOperations operation = baseMapper.selectById(id);
        if (operation == null) {
            throw new JeecgBootException("作业单不存在");
        }
        if (operation.getStatus() != null && operation.getStatus() == WmsOperationStatusEnum.EXECUTED.getCode()) {
            throw new JeecgBootException("已执行的作业单不能取消");
        }
        operation.setStatus(WmsOperationStatusEnum.CANCELLED.getCode()).setUpdateTime(new Date());
        baseMapper.updateById(operation);
    }

    @Override
    public List<WmsWarehouseOperationsDetails> listDetails(Long operationId) {
        return baseMapper.selectDetailsByOperationId(operationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsWarehouseOperations generateReplenish(Integer warehouseId, List<Long> skuIds) {
        if (warehouseId == null) {
            throw new JeecgBootException("请指定需要补货的仓库");
        }
        // 拣货区库存低于 min_stock 的 SKU，按 max_stock - 拣货区库存 计算补货量
        List<WmsWarehouseArea> pickingAreas = areaMapper.selectList(new LambdaQueryWrapper<WmsWarehouseArea>()
                .eq(WmsWarehouseArea::getWarehouseId, warehouseId)
                .eq(WmsWarehouseArea::getCategory, AREA_PICKING));
        if (pickingAreas.isEmpty()) {
            throw new JeecgBootException("当前仓库没有拣货区(PICKING)，无需补货");
        }
        List<Integer> pickingAreaIds = pickingAreas.stream().map(WmsWarehouseArea::getId).collect(Collectors.toList());

        WmsOperationForm form = new WmsOperationForm();
        form.setOperationType(OP_TYPE_REPLENISH);
        form.setWarehouseId(warehouseId);
        form.setRemark("系统补货：拣货区低于补货下限时自动生成");
        List<WmsOperationDetailForm> lines = new ArrayList<>();

        List<WmsItemSku> candidates;
        if (skuIds != null && !skuIds.isEmpty()) {
            candidates = skuMapper.selectBatchIds(skuIds);
        } else {
            candidates = skuMapper.selectList(new LambdaQueryWrapper<WmsItemSku>()
                    .isNotNull(WmsItemSku::getMinStock)
                    .gt(WmsItemSku::getMinStock, 0));
        }

        for (WmsItemSku sku : candidates) {
            int minStock = sku.getMinStock() == null ? 0 : sku.getMinStock();
            if (minStock <= 0) {
                continue;
            }
            int maxStock = sku.getMaxStock() == null ? 0 : sku.getMaxStock();
            long pickingAvailable = pickingStock(warehouseId, pickingAreaIds, sku.getId());
            if (pickingAvailable >= minStock) {
                continue;
            }
            int need = (maxStock > 0 ? maxStock : minStock) - (int) pickingAvailable;
            if (need <= 0) {
                continue;
            }
            // FEFO：从存储区取效期最近、批次号最小的批次
            WmsInventory source = findStorageSource(warehouseId, sku.getId(), need);
            if (source == null) {
                continue;
            }
            WmsWarehouseAreaLoca pickingLoca = findPickingLoca(warehouseId, pickingAreaIds, sku.getId(), source.getBatchNumber());
            WmsOperationDetailForm line = new WmsOperationDetailForm();
            line.setSkuId(sku.getId());
            line.setBatchNumber(source.getBatchNumber());
            line.setConsignorId(source.getConsignorId());
            line.setQualityStatus(source.getQualityStatus());
            line.setSourceWarehouseAreaLocaId(source.getWarehouseAreaLocaId());
            line.setTargetWarehouseAreaLocaId(pickingLoca == null ? null : pickingLoca.getId());
            line.setCount(Math.min(need, source.getAvailableCount() == null ? 0 : source.getAvailableCount()));
            if (line.getCount() > 0) {
                lines.add(line);
            }
        }

        if (lines.isEmpty()) {
            throw new JeecgBootException("拣货区库存均不低于补货下限，无需补货");
        }
        form.setDetails(lines);
        return createOperation(form);
    }

    /**
     * 拣货区当前可用库存合计。
     */
    private long pickingStock(Integer warehouseId, List<Integer> pickingAreaIds, Long skuId) {
        List<WmsInventory> rows = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseId, warehouseId)
                .in(WmsInventory::getWarehouseAreaId, pickingAreaIds)
                .eq(WmsInventory::getSkuId, skuId)
                .gt(WmsInventory::getTotalCount, 0));
        return rows.stream().mapToLong(r -> r.getAvailableCount() == null ? 0 : r.getAvailableCount()).sum();
    }

    /**
     * FEFO 选取存储区的补货来源：效期最近的优先，同效期按批次号升序。
     */
    private WmsInventory findStorageSource(Integer warehouseId, Long skuId, int need) {
        List<WmsInventory> rows = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseId, warehouseId)
                .eq(WmsInventory::getSkuId, skuId)
                .gt(WmsInventory::getAvailableCount, 0)
                .eq(WmsInventory::getQualityStatus, 1));
        if (rows.isEmpty()) {
            return null;
        }
        List<WmsInventory> storageRows = new ArrayList<>();
        for (WmsInventory row : rows) {
            WmsWarehouseArea area = areaMapper.selectById(row.getWarehouseAreaId());
            if (area != null && AREA_STORAGE.equals(area.getCategory())) {
                storageRows.add(row);
            }
        }
        if (storageRows.isEmpty()) {
            return null;
        }
        storageRows.sort(Comparator
                .comparing((WmsInventory row) -> row.getExpireDate() == null ? "9999-12-31" : row.getExpireDate().toString())
                .thenComparing(row -> row.getBatchNumber() == null ? "" : row.getBatchNumber()));
        return storageRows.get(0);
    }

    /**
     * 为补货挑一个拣货区库位：优先同 SKU 同批次的现有库位，其次是空库位。
     */
    private WmsWarehouseAreaLoca findPickingLoca(Integer warehouseId, List<Integer> pickingAreaIds, Long skuId, String batchNumber) {
        List<WmsWarehouseAreaLoca> locas = locaMapper.selectList(new LambdaQueryWrapper<WmsWarehouseAreaLoca>()
                .eq(WmsWarehouseAreaLoca::getWarehouseId, warehouseId)
                .in(WmsWarehouseAreaLoca::getWarehouseAreaId, pickingAreaIds)
                .notIn(WmsWarehouseAreaLoca::getLocaStatus, 3, 4)
                .orderByAsc(WmsWarehouseAreaLoca::getId));
        for (WmsWarehouseAreaLoca loca : locas) {
            boolean sameBatchOccupied = inventoryMapper.selectCount(new LambdaQueryWrapper<WmsInventory>()
                    .eq(WmsInventory::getWarehouseAreaLocaId, loca.getId())
                    .eq(WmsInventory::getSkuId, skuId)
                    .eq(WmsInventory::getBatchNumber, batchNumber == null ? "" : batchNumber)
                    .gt(WmsInventory::getTotalCount, 0)) > 0;
            if (sameBatchOccupied) {
                return loca;
            }
        }
        for (WmsWarehouseAreaLoca loca : locas) {
            long occupied = inventoryMapper.selectCount(new LambdaQueryWrapper<WmsInventory>()
                    .eq(WmsInventory::getWarehouseAreaLocaId, loca.getId())
                    .gt(WmsInventory::getTotalCount, 0));
            if (occupied == 0) {
                return loca;
            }
        }
        return null;
    }

    private Integer resolveWarehouseId(Integer locaId) {
        WmsWarehouseAreaLoca loca = locaMapper.selectById(locaId);
        return loca == null ? null : loca.getWarehouseId();
    }

    private Integer resolveAreaId(Integer locaId) {
        WmsWarehouseAreaLoca loca = locaMapper.selectById(locaId);
        return loca == null ? null : loca.getWarehouseAreaId();
    }

    /**
     * 作业单号：MV/RP + yyyyMMddHHmmss + 3 位随机数。
     */
    private String nextJobCode(int operationType) {
        String prefix = operationType == OP_TYPE_REPLENISH ? "RP" : "MV";
        String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        return prefix + ts + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}
