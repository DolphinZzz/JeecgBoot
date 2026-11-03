package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsItemSpu;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.entity.WmsWarehouseOperations;
import org.jeecg.modules.wms.entity.WmsWarehouseOperationsDetails;
import org.jeecg.modules.wms.enums.WmsInventoryBizTypeEnum;
import org.jeecg.modules.wms.enums.WmsOperationStatusEnum;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.mapper.WmsItemSkuMapper;
import org.jeecg.modules.wms.mapper.WmsItemSpuMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseOperationsDetailsMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseOperationsMapper;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.service.IWmsLocaCompatibilityService;
import org.jeecg.modules.wms.service.IWmsWarehouseOperationsService;
import org.jeecg.modules.wms.vo.WmsAreaMigrationDetailVO;
import org.jeecg.modules.wms.vo.WmsAreaMigrationForm;
import org.jeecg.modules.wms.vo.WmsAreaMigrationPreviewVO;
import org.jeecg.modules.wms.vo.WmsInventoryDimension;
import org.jeecg.modules.wms.vo.WmsLocaOptionVO;
import org.jeecg.modules.wms.vo.WmsOperationDetailForm;
import org.jeecg.modules.wms.vo.WmsOperationForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private WmsItemSpuMapper spuMapper;

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
            throw new JeecgBootException("请指定源仓库");
        }
        Integer targetWarehouseId = form.getTargetWarehouseId() != null ? form.getTargetWarehouseId() : form.getWarehouseId();

        WmsWarehouseOperations operation = new WmsWarehouseOperations()
                .setJobCode(nextJobCode(operationType))
                .setWarehouseId(form.getWarehouseId())
                .setTargetWarehouseId(targetWarehouseId)
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
        if (operation.getWarehouseId() != null && sourceLoca.getWarehouseId() != null
                && !sourceLoca.getWarehouseId().equals(operation.getWarehouseId())) {
            throw new JeecgBootException(String.format("移出库位[%s]不属于当前源仓库", sourceLoca.getLocaCode()));
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

        // 确定目标仓库ID（明细行优先，其次取作业单目标仓库，最后退回源仓库）
        Integer targetWhId = line.getTargetWarehouseId() != null ? line.getTargetWarehouseId() : operation.getTargetWarehouseId();
        if (targetWhId == null) {
            targetWhId = operation.getWarehouseId();
        }

        if (line.getTargetWarehouseAreaLocaId() != null) {
            WmsWarehouseAreaLoca targetLoca = locaMapper.selectById(line.getTargetWarehouseAreaLocaId());
            if (targetLoca != null && targetWhId != null && targetLoca.getWarehouseId() != null
                    && !targetLoca.getWarehouseId().equals(targetWhId)) {
                throw new JeecgBootException(String.format("移入库位[%s]不属于目标仓库", targetLoca.getLocaCode()));
            }
            locaCompatibilityService.checkInbound(line.getTargetWarehouseAreaLocaId(), line.getSkuId(), line.getBatchNumber(), line.getCount(), line.getQualityStatus());
            if (targetLoca != null && targetLoca.getWarehouseId() != null) {
                targetWhId = targetLoca.getWarehouseId();
            }
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
                .setTargetWarehouseId(targetWhId)
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
            locaCompatibilityService.checkInbound(detail.getTargetWarehouseAreaLocaId(), detail.getSkuId(),
                    detail.getBatchNumber(), count, detail.getQualityStatus());

            Integer sourceWhId = resolveWarehouseId(detail.getSourceWarehouseAreaLocaId());
            Integer targetWhId = detail.getTargetWarehouseId() != null ? detail.getTargetWarehouseId()
                    : resolveWarehouseId(detail.getTargetWarehouseAreaLocaId());
            boolean isCrossWarehouse = sourceWhId != null && targetWhId != null && !sourceWhId.equals(targetWhId);
            String moveRemark = isCrossWarehouse ? "跨仓调拨" : "移库移位";

            WmsInventoryDimension sourceDim = new WmsInventoryDimension()
                    .setWarehouseId(sourceWhId)
                    .setWarehouseAreaId(detail.getSourceWarehouseAreaId())
                    .setWarehouseAreaLocaId(detail.getSourceWarehouseAreaLocaId())
                    .setConsignorId(detail.getConsignorId())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setQualityStatus(detail.getQualityStatus());

            // 查出源库存行，以便在目标库位新建时继承生产日期与有效期
            WmsInventory sourceInv = inventoryService.getByDimension(sourceDim);

            // 移出：先扣源库位
            inventoryService.applyChange(sourceDim, -count, 0, 0, moveOutBiz, operation.getJobCode(),
                    moveRemark + "移出到库位ID " + detail.getTargetWarehouseAreaLocaId());

            // 移入：目标库位同维度增加（生产/有效期优先继承目标库位既有属性，否则沿用源库存行）
            WmsInventory target = inventoryService.getByDimension(new WmsInventoryDimension()
                    .setWarehouseId(targetWhId)
                    .setWarehouseAreaLocaId(detail.getTargetWarehouseAreaLocaId())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setConsignorId(detail.getConsignorId())
                    .setQualityStatus(detail.getQualityStatus()));

            Date finalProdDate = target != null && target.getProdDate() != null ? target.getProdDate()
                    : (sourceInv != null ? sourceInv.getProdDate() : null);
            Date finalExpireDate = target != null && target.getExpireDate() != null ? target.getExpireDate()
                    : (sourceInv != null ? sourceInv.getExpireDate() : null);

            WmsInventoryDimension targetDim = new WmsInventoryDimension()
                    .setWarehouseId(targetWhId)
                    .setWarehouseAreaId(resolveAreaId(detail.getTargetWarehouseAreaLocaId()))
                    .setWarehouseAreaLocaId(detail.getTargetWarehouseAreaLocaId())
                    .setConsignorId(detail.getConsignorId())
                    .setSkuId(detail.getSkuId())
                    .setBatchNumber(detail.getBatchNumber())
                    .setQualityStatus(detail.getQualityStatus())
                    .setProdDate(finalProdDate)
                    .setExpireDate(finalExpireDate);
            inventoryService.applyChange(targetDim, count, 0, 0, moveInBiz, operation.getJobCode(),
                    moveRemark + "移入，来源库位ID " + detail.getSourceWarehouseAreaLocaId());

            // 刷新两边库位占用状态
            locaCompatibilityService.refreshLocaOccupancy(detail.getSourceWarehouseAreaLocaId());
            locaCompatibilityService.refreshLocaOccupancy(detail.getTargetWarehouseAreaLocaId());

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

    @Override
    public WmsAreaMigrationPreviewVO previewAreaMigration(Integer sourceWarehouseId, Integer sourceWarehouseAreaId,
                                                           Integer targetWarehouseId, Integer targetWarehouseAreaId) {
        if (sourceWarehouseId == null || sourceWarehouseAreaId == null || targetWarehouseId == null || targetWarehouseAreaId == null) {
            throw new JeecgBootException("请完整选择源仓库、源库区、目标仓库与目标库区");
        }
        if (sourceWarehouseAreaId.equals(targetWarehouseAreaId)) {
            throw new JeecgBootException("源库区与目标库区不能相同");
        }

        WmsWarehouseArea sourceArea = areaMapper.selectById(sourceWarehouseAreaId);
        WmsWarehouseArea targetArea = areaMapper.selectById(targetWarehouseAreaId);
        if (sourceArea == null || targetArea == null) {
            throw new JeecgBootException("指定的源库区或目标库区不存在");
        }

        // 基础温区兼容检查
        String srcCondition = sourceArea.getStorageCondition();
        String tgtCondition = targetArea.getStorageCondition();
        if (oConvertUtils.isNotEmpty(srcCondition) && oConvertUtils.isNotEmpty(tgtCondition) && !srcCondition.equalsIgnoreCase(tgtCondition)) {
            throw new JeecgBootException(String.format("库区温区不匹配：源库区温区为[%s]，目标库区温区为[%s]，无法执行整区迁移",
                    srcCondition, tgtCondition));
        }

        // 1. 扫描源库区下在库库存行（每条库存对应一个库位占用的货物）
        List<WmsInventory> sourceInvs = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseAreaId, sourceWarehouseAreaId)
                .gt(WmsInventory::getTotalCount, 0)
                .orderByAsc(WmsInventory::getWarehouseAreaLocaId));

        WmsAreaMigrationPreviewVO preview = new WmsAreaMigrationPreviewVO()
                .setSourceWarehouseId(sourceWarehouseId)
                .setSourceWarehouseAreaId(sourceWarehouseAreaId)
                .setSourceAreaName(sourceArea.getName())
                .setTargetWarehouseId(targetWarehouseId)
                .setTargetWarehouseAreaId(targetWarehouseAreaId)
                .setTargetAreaName(targetArea.getName());

        if (sourceInvs.isEmpty()) {
            return preview
                    .setCanMigrate(false)
                    .setSourceOccupiedLocaCount(0)
                    .setSourceTotalItemCount(0)
                    .setTargetEmptyLocaCount(0)
                    .setMessage(String.format("源库区[%s]当前没有在库货物，无需执行迁移", sourceArea.getName()))
                    .setDetails(Collections.emptyList());
        }

        // 2. 扫描目标库区下所有空闲可用库位
        List<WmsWarehouseAreaLoca> targetAllLocas = locaMapper.selectList(new LambdaQueryWrapper<WmsWarehouseAreaLoca>()
                .eq(WmsWarehouseAreaLoca::getWarehouseAreaId, targetWarehouseAreaId)
                .eq(WmsWarehouseAreaLoca::getIsDel, 1)
                .eq(WmsWarehouseAreaLoca::getLocaStatus, 1)
                .orderByAsc(WmsWarehouseAreaLoca::getLocaCode));

        List<WmsInventory> targetOccupiedInvs = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseAreaId, targetWarehouseAreaId)
                .gt(WmsInventory::getTotalCount, 0));
        Set<Integer> targetOccupiedLocaIds = targetOccupiedInvs.stream()
                .map(WmsInventory::getWarehouseAreaLocaId)
                .collect(Collectors.toSet());

        List<WmsWarehouseAreaLoca> targetEmptyLocas = targetAllLocas.stream()
                .filter(l -> !targetOccupiedLocaIds.contains(l.getId()))
                .collect(Collectors.toList());

        List<WmsLocaOptionVO> candidateOptions = targetEmptyLocas.stream()
                .map(l -> new WmsLocaOptionVO().setLocaId(l.getId()).setLocaCode(l.getLocaCode()))
                .collect(Collectors.toList());

        int sourceOccupiedCount = sourceInvs.size();
        int totalItemCount = sourceInvs.stream().mapToInt(WmsInventory::getTotalCount).sum();
        int targetEmptyCount = targetEmptyLocas.size();
        boolean canMigrate = targetEmptyCount >= sourceOccupiedCount;

        // 3. 批量查询关联的主数据（库位编码、SKU编码、SPU标题）
        Set<Integer> sourceLocaIds = sourceInvs.stream().map(WmsInventory::getWarehouseAreaLocaId).collect(Collectors.toSet());
        Map<Integer, String> locaCodeMap = locaMapper.selectBatchIds(sourceLocaIds).stream()
                .collect(Collectors.toMap(WmsWarehouseAreaLoca::getId, WmsWarehouseAreaLoca::getLocaCode, (k1, k2) -> k1));

        Set<Long> skuIds = sourceInvs.stream().map(WmsInventory::getSkuId).collect(Collectors.toSet());
        Map<Long, WmsItemSku> skuMap = skuIds.isEmpty() ? Collections.emptyMap() :
                skuMapper.selectBatchIds(skuIds).stream().collect(Collectors.toMap(WmsItemSku::getId, s -> s, (k1, k2) -> k1));
        Set<Long> spuIds = skuMap.values().stream().map(WmsItemSku::getSpuId).filter(id -> id != null).collect(Collectors.toSet());
        Map<Long, String> spuNameMap = spuIds.isEmpty() ? Collections.emptyMap() :
                spuMapper.selectBatchIds(spuIds).stream().collect(Collectors.toMap(WmsItemSpu::getId, WmsItemSpu::getTitle, (k1, k2) -> k1));

        // 4. 一对一贪心分配与明细组装
        List<WmsAreaMigrationDetailVO> details = new ArrayList<>();
        List<String> missingSourceLocas = new ArrayList<>();
        for (int i = 0; i < sourceInvs.size(); i++) {
            WmsInventory inv = sourceInvs.get(i);
            String sourceLocaCode = locaCodeMap.getOrDefault(inv.getWarehouseAreaLocaId(), "LOCA-" + inv.getWarehouseAreaLocaId());
            WmsAreaMigrationDetailVO d = new WmsAreaMigrationDetailVO()
                    .setSourceWarehouseAreaLocaId(inv.getWarehouseAreaLocaId())
                    .setSourceLocaCode(sourceLocaCode)
                    .setSkuId(inv.getSkuId())
                    .setBatchNumber(inv.getBatchNumber())
                    .setConsignorId(inv.getConsignorId())
                    .setQualityStatus(inv.getQualityStatus() == null ? 1 : inv.getQualityStatus())
                    .setCount(inv.getTotalCount())
                    .setCandidateTargetLocas(candidateOptions);

            WmsItemSku sku = skuMap.get(inv.getSkuId());
            d.setSkuCode(sku != null && sku.getCode() != null ? sku.getCode() : String.valueOf(inv.getSkuId()));
            String spuName = sku != null && sku.getSpuId() != null ? spuNameMap.get(sku.getSpuId()) : null;
            d.setSpuTitle(spuName != null ? spuName : "商品-" + inv.getSkuId());

            if (i < targetEmptyLocas.size()) {
                WmsWarehouseAreaLoca targetLoca = targetEmptyLocas.get(i);
                d.setTargetWarehouseAreaLocaId(targetLoca.getId());
                d.setTargetLocaCode(targetLoca.getLocaCode());
                d.setMatchStatus("MATCHED");
            } else {
                d.setMatchStatus("MISSING");
                missingSourceLocas.add(sourceLocaCode);
            }
            details.add(d);
        }

        int missingCount = Math.max(0, sourceOccupiedCount - targetEmptyCount);
        String msg;
        if (canMigrate) {
            msg = String.format("预检通过：源库区共有 %d 个有货库位（在库 %d 件货物），目标库区有 %d 个空闲库位，已自动完成一对一智能匹配！",
                    sourceOccupiedCount, totalItemCount, targetEmptyCount);
        } else {
            msg = String.format("目标库区空闲库位不足：源库区需迁移 %d 个库位，目标库区仅有 %d 个空闲库位，尚缺 %d 个空位！",
                    sourceOccupiedCount, targetEmptyCount, missingCount);
        }

        return preview
                .setCanMigrate(canMigrate)
                .setMessage(msg)
                .setSourceOccupiedLocaCount(sourceOccupiedCount)
                .setSourceTotalItemCount(totalItemCount)
                .setTargetEmptyLocaCount(targetEmptyCount)
                .setMissingLocaCount(missingCount)
                .setMissingSourceLocas(missingSourceLocas)
                .setDetails(details);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeAreaMigration(WmsAreaMigrationForm form) {
        if (form == null || form.getDetails() == null || form.getDetails().isEmpty()) {
            throw new JeecgBootException("迁移明细不能为空");
        }
        if (form.getSourceWarehouseId() == null || form.getSourceWarehouseAreaId() == null) {
            throw new JeecgBootException("请指定源仓库与源库区");
        }
        if (form.getTargetWarehouseId() == null || form.getTargetWarehouseAreaId() == null) {
            throw new JeecgBootException("请指定目标仓库与目标库区");
        }

        Set<Integer> targetLocaIds = new HashSet<>();
        for (WmsOperationDetailForm line : form.getDetails()) {
            if (line.getTargetWarehouseAreaLocaId() == null) {
                throw new JeecgBootException("存在未指定目标库位的明细行，请先补全或由系统重新匹配");
            }
            if (!targetLocaIds.add(line.getTargetWarehouseAreaLocaId())) {
                WmsWarehouseAreaLoca l = locaMapper.selectById(line.getTargetWarehouseAreaLocaId());
                String code = l != null ? l.getLocaCode() : String.valueOf(line.getTargetWarehouseAreaLocaId());
                throw new JeecgBootException(String.format("目标库位[%s]被重复指定，每个商品必须迁移到独立的空库位！", code));
            }
        }

        WmsWarehouseArea sourceArea = areaMapper.selectById(form.getSourceWarehouseAreaId());
        WmsWarehouseArea targetArea = areaMapper.selectById(form.getTargetWarehouseAreaId());
        String defaultRemark = String.format("整库区迁移：从库区[%s]整体搬迁到库区[%s]",
                sourceArea != null ? sourceArea.getName() : form.getSourceWarehouseAreaId(),
                targetArea != null ? targetArea.getName() : form.getTargetWarehouseAreaId());

        WmsOperationForm opForm = new WmsOperationForm();
        opForm.setOperationType(OP_TYPE_MOVE);
        opForm.setWarehouseId(form.getSourceWarehouseId());
        opForm.setRemark(oConvertUtils.isNotEmpty(form.getRemark()) ? form.getRemark() : defaultRemark);
        opForm.setDetails(form.getDetails());

        WmsWarehouseOperations operation = createOperation(opForm);

        if (Boolean.TRUE.equals(form.getAutoExecute())) {
            Map<String, Object> execResult = execute(operation.getId());
            execResult.put("operationId", operation.getId());
            execResult.put("jobCode", operation.getJobCode());
            execResult.put("message", String.format("整库区迁移成功！作业单[%s]已自动执行，共迁移 %d 行（%s 件），源库区已全部腾空。",
                    operation.getJobCode(), form.getDetails().size(), execResult.getOrDefault("movedCount", 0)));
            return execResult;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operationId", operation.getId());
        result.put("jobCode", operation.getJobCode());
        result.put("message", String.format("整库区迁移作业单[%s]已生成，请在移库单列表核对后点击执行。", operation.getJobCode()));
        return result;
    }
}
