package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.dto.ErpOutboundOrderCreateDto;
import org.jeecg.modules.wms.dto.ErpOutboundOrderDetailDto;
import org.jeecg.modules.wms.entity.*;
import org.jeecg.modules.wms.enums.*;
import org.jeecg.modules.wms.mapper.*;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.service.IWmsOutboundOrderService;
import org.jeecg.modules.wms.service.IWmsWarehouseService;
import org.jeecg.modules.wms.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 出库单服务实现（出库闭环全流程落地）
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Slf4j
@Service
public class WmsOutboundOrderServiceImpl extends ServiceImpl<WmsOutboundOrderMapper, WmsOutboundOrder>
        implements IWmsOutboundOrderService {

    @Autowired
    private WmsOutboundOrderDetailsMapper outboundOrderDetailsMapper;

    @Autowired
    private WmsOutboundOrderPackageMapper outboundOrderPackageMapper;

    @Autowired
    private WmsOutboundOrderWeightsMapper outboundOrderWeightsMapper;

    @Autowired
    private IWmsInventoryService inventoryService;

    @Autowired
    private WmsInventoryMapper inventoryMapper;

    @Autowired
    private IWmsWarehouseService warehouseService;

    @Autowired
    private WmsItemSkuMapper itemSkuMapper;

    @Autowired
    private WmsItemSpuMapper itemSpuMapper;

    @Autowired
    private WmsItemSnMapper itemSnMapper;

    @Autowired
    private WmsWarehouseOperationsMapper operationsMapper;

    @Autowired
    private WmsWarehouseOperationsDetailsMapper operationsDetailsMapper;

    @Autowired
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Override
    public IPage<WmsOutboundPage> queryOutboundPage(WmsOutboundQuery query, Integer pageNo, Integer pageSize) {
        Page<WmsOutboundPage> page = new Page<>(pageNo, pageSize);
        if (query == null) {
            query = new WmsOutboundQuery();
        }
        return baseMapper.selectOutboundPage(page, query);
    }

    @Override
    public WmsOutboundPage getOutboundDetail(Long orderId) {
        WmsOutboundOrder order = getById(orderId);
        if (order == null) {
            throw new JeecgBootException("出库单不存在: id=" + orderId);
        }

        WmsOutboundQuery query = new WmsOutboundQuery();
        query.setOutboundCode(order.getOutboundCode());
        IPage<WmsOutboundPage> res = baseMapper.selectOutboundPage(new Page<>(1, 1), query);
        if (res.getRecords().isEmpty()) {
            throw new JeecgBootException("查询出库单详情失败: id=" + orderId);
        }
        return res.getRecords().get(0);
    }

    @Override
    public List<WmsOutboundDetailView> listDetails(Long orderId) {
        return outboundOrderDetailsMapper.selectDetailViewsByOrderId(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsOutboundOrder createOutboundOrder(WmsOutboundCreateForm form) {
        // 1. 校验仓库类型与规则（决策 D1）
        WmsWarehouse warehouse = warehouseService.getById(form.getWarehouseId());
        if (warehouse == null) {
            throw new JeecgBootException("指定仓库不存在: id=" + form.getWarehouseId());
        }

        WmsWarehouseTypeEnum whType = WmsWarehouseTypeEnum.of(warehouse.getWarehouseType());
        if (Objects.equals(form.getOutboundType(), WmsOutboundTypeEnum.MES_WORK_ORDER.getCode())) {
            if (whType.requireMesWorkOrder() && (form.getMesWorkOrderNo() == null || form.getMesWorkOrderNo().trim().isEmpty())) {
                throw new JeecgBootException("工业制造仓的 MES 工单领料出库必须绑定生产工单号(mes_work_order_no)");
            }
        }
        if (Objects.equals(form.getOutboundType(), WmsOutboundTypeEnum.SALE.getCode())) {
            if (form.getCustomerId() == null) {
                throw new JeecgBootException("销售出库必须选择客户(customer_id)");
            }
        }

        // 2. 生成出库单号
        String datePrefix = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String outboundCode = generateOutboundCode(datePrefix);

        // 3. 计算计划总件数
        int totalCount = 0;
        for (WmsOutboundDetailForm line : form.getDetails()) {
            totalCount += line.getOutCount();
        }

        // 4. 保存单头
        WmsOutboundOrder order = new WmsOutboundOrder();
        order.setOutboundCode(outboundCode)
                .setOutboundType(form.getOutboundType())
                .setPickMode(form.getPickMode() != null ? form.getPickMode() : WmsPickModeEnum.PIECE_PICK.getCode())
                .setWarehouseId(form.getWarehouseId())
                .setConsignorId(form.getConsignorId())
                .setCustomerId(form.getCustomerId())
                .setErpOrderNo(form.getErpOrderNo())
                .setMesWorkOrderNo(form.getMesWorkOrderNo())
                .setLogisticsCompanyId(form.getLogisticsCompanyId())
                .setStatus(WmsOutboundStatusEnum.PENDING_ALLOCATE.getCode())
                .setTotalCount(totalCount)
                .setTotalWeight(0)
                .setTotalVolume(0)
                .setPrice(0)
                .setCreateTime(new Date());

        save(order);

        // 5. 保存明细行
        Date now = new Date();
        for (WmsOutboundDetailForm line : form.getDetails()) {
            WmsOutboundOrderDetails detail = new WmsOutboundOrderDetails();
            detail.setOutboundOrderId(order.getId())
                    .setSkuId(line.getSkuId())
                    .setConsignorId(form.getConsignorId())
                    .setBatchNumber(line.getBatchNumber())
                    .setOutCount(line.getOutCount())
                    .setAllocatedCount(0)
                    .setPickedCount(0)
                    .setReviewCount(0)
                    .setReviewStatus(WmsReviewStatusEnum.UNREVIEWED.getCode())
                    .setShortageHandle(line.getShortageHandle() != null ? line.getShortageHandle() : WmsShortageHandleEnum.REMOVE_SHORTAGE.getCode())
                    .setPickStatus(1) // 未拣货
                    .setTargetWarehouseId(form.getWarehouseId())
                    .setTargetWarehouseAreaId(0)
                    .setTargetWarehouseAreaLocaId(0)
                    .setCreateTime(now);

            outboundOrderDetailsMapper.insert(detail);
        }

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsOutboundOrderResultVo createFromErp(ErpOutboundOrderCreateDto dto) {
        if (dto == null) {
            throw new JeecgBootException("请求体不能为空");
        }
        if (dto.getWarehouseId() == null) {
            throw new JeecgBootException("仓库ID不能为空");
        }
        if (dto.getConsignorId() == null) {
            throw new JeecgBootException("货主ID不能为空");
        }
        if (dto.getDetails() == null || dto.getDetails().isEmpty()) {
            throw new JeecgBootException("出库明细不能为空");
        }

        // 防重检查
        if (dto.getErpOrderNo() != null && !dto.getErpOrderNo().trim().isEmpty()) {
            Long count = count(new LambdaQueryWrapper<WmsOutboundOrder>()
                    .eq(WmsOutboundOrder::getErpOrderNo, dto.getErpOrderNo().trim())
                    .eq(WmsOutboundOrder::getIsDel, 1));
            if (count > 0) {
                throw new JeecgBootException("ERP单号[" + dto.getErpOrderNo() + "]已存在，请勿重复推送");
            }
        }

        WmsOutboundCreateForm form = new WmsOutboundCreateForm();
        form.setOutboundType(dto.getOutboundType() != null ? dto.getOutboundType() : WmsOutboundTypeEnum.SALE.getCode());
        form.setWarehouseId(dto.getWarehouseId());
        form.setConsignorId(dto.getConsignorId());
        form.setCustomerId(dto.getCustomerId());
        form.setErpOrderNo(dto.getErpOrderNo());
        form.setMesWorkOrderNo(dto.getMesWorkOrderNo());
        form.setPickMode(dto.getPickMode() != null ? dto.getPickMode() : WmsPickModeEnum.PIECE_PICK.getCode());
        form.setLogisticsCompanyId(dto.getLogisticsCompanyId());

        List<WmsOutboundDetailForm> detailForms = new ArrayList<>();
        for (ErpOutboundOrderDetailDto d : dto.getDetails()) {
            WmsOutboundDetailForm df = new WmsOutboundDetailForm();
            df.setSkuId(d.getSkuId());
            df.setOutCount(d.getOutCount());
            df.setBatchNumber(d.getBatchNumber());
            df.setShortageHandle(d.getShortageHandle() != null ? d.getShortageHandle() : WmsShortageHandleEnum.REMOVE_SHORTAGE.getCode());
            detailForms.add(df);
        }
        form.setDetails(detailForms);

        WmsOutboundOrder order = createOutboundOrder(form);

        WmsOutboundOrderResultVo vo = new WmsOutboundOrderResultVo();
        vo.setId(order.getId());
        vo.setOutboundCode(order.getOutboundCode());
        vo.setStatus(order.getStatus());
        vo.setTotalCount(order.getTotalCount());
        vo.setDetailRows(dto.getDetails().size());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsOutboundAllocateResultVo allocateInventory(Long orderId) {
        WmsOutboundOrder order = getById(orderId);
        if (order == null) {
            throw new JeecgBootException("出库单不存在: id=" + orderId);
        }

        if (order.getStatus() != WmsOutboundStatusEnum.PENDING_ALLOCATE.getCode()
                && order.getStatus() != WmsOutboundStatusEnum.WAIT_REPLENISH.getCode()) {
            throw new JeecgBootException("只有待分配(10)或待补货(30)状态的出库单允许执行库存分配，当前状态: " + order.getStatus());
        }

        List<WmsOutboundOrderDetails> details = outboundOrderDetailsMapper.selectList(
                new LambdaQueryWrapper<WmsOutboundOrderDetails>()
                        .eq(WmsOutboundOrderDetails::getOutboundOrderId, orderId)
                        .eq(WmsOutboundOrderDetails::getIsDel, 1));

        if (details.isEmpty()) {
            throw new JeecgBootException("出库单明细为空，无法分配库存");
        }

        // 收集 SKU 信息与临期天数
        Set<Long> skuIds = details.stream().map(WmsOutboundOrderDetails::getSkuId).collect(Collectors.toSet());
        List<WmsItemSku> skus = itemSkuMapper.selectBatchIds(skuIds);
        Map<Long, WmsItemSku> skuMap = skus.stream().collect(Collectors.toMap(WmsItemSku::getId, s -> s));

        Set<Long> spuIds = skus.stream().map(WmsItemSku::getSpuId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WmsItemSpu> spuMap = new HashMap<>();
        if (!spuIds.isEmpty()) {
            List<WmsItemSpu> spus = itemSpuMapper.selectBatchIds(spuIds);
            spuMap = spus.stream().collect(Collectors.toMap(WmsItemSpu::getId, s -> s));
        }

        boolean hasShortage = false;
        boolean hasSuspendStrategy = false;
        List<WmsOutboundAllocateResultVo.WmsLineAllocateResult> lineResults = new ArrayList<>();
        // 记录本轮已锁定的明细分配记录（用于整单挂起时原子回滚）
        List<AllocatedRecord> currentAllocations = new ArrayList<>();

        for (WmsOutboundOrderDetails detail : details) {
            int alreadyAllocated = detail.getAllocatedCount() != null ? detail.getAllocatedCount() : 0;
            int need = detail.getOutCount() - alreadyAllocated;

            WmsItemSku sku = skuMap.get(detail.getSkuId());
            WmsItemSpu spu = sku != null && sku.getSpuId() != null ? spuMap.get(sku.getSpuId()) : null;
            Integer nearExpireDays = spu != null ? spu.getNearExpireDays() : null;

            WmsOutboundAllocateResultVo.WmsLineAllocateResult lineVo = new WmsOutboundAllocateResultVo.WmsLineAllocateResult();
            lineVo.setDetailId(detail.getId());
            lineVo.setSkuId(detail.getSkuId());
            lineVo.setSkuCode(sku != null ? sku.getCode() : "");
            lineVo.setSkuName(spu != null ? spu.getTitle() : (sku != null ? sku.getTitle() : ""));
            lineVo.setPlanCount(detail.getOutCount());
            lineVo.setAllocatedCount(alreadyAllocated);

            if (need <= 0) {
                lineVo.setShortageCount(0);
                lineVo.setIsShortage(false);
                lineResults.add(lineVo);
                continue;
            }

            // 检索良品在库可用库存（FEFO近效期优先：按 expire_date ASC, batch_number ASC, create_time ASC）
            LambdaQueryWrapper<WmsInventory> invQw = new LambdaQueryWrapper<WmsInventory>()
                    .eq(WmsInventory::getWarehouseId, order.getWarehouseId())
                    .eq(WmsInventory::getSkuId, detail.getSkuId())
                    .eq(WmsInventory::getQualityStatus, 1) // 1良品
                    .gt(WmsInventory::getAvailableCount, 0)
                    .eq(WmsInventory::getIsDel, 1);

            if (order.getConsignorId() != null) {
                invQw.eq(WmsInventory::getConsignorId, order.getConsignorId());
            }

            // MES工单领料场景若指定了批次号，强匹配批次
            if (detail.getBatchNumber() != null && !detail.getBatchNumber().trim().isEmpty()) {
                invQw.eq(WmsInventory::getBatchNumber, detail.getBatchNumber().trim());
            }

            invQw.orderByAsc(WmsInventory::getExpireDate)
                    .orderByAsc(WmsInventory::getBatchNumber)
                    .orderByAsc(WmsInventory::getCreateTime);

            List<WmsInventory> invList = inventoryMapper.selectList(invQw);

            int allocatedThisLine = 0;
            LocalDate today = LocalDate.now();

            for (WmsInventory inv : invList) {
                // 临期禁出校验（决策 D5）
                if (nearExpireDays != null && nearExpireDays > 0 && inv.getExpireDate() != null) {
                    LocalDate expDate = inv.getExpireDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    long daysToExpiry = ChronoUnit.DAYS.between(today, expDate);
                    if (daysToExpiry <= nearExpireDays) {
                        log.warn("批次[{}]临期天数[{}天] <= 禁出阈值[{}天]，跳过出库分配", inv.getBatchNumber(), daysToExpiry, nearExpireDays);
                        continue;
                    }
                }

                int take = Math.min(inv.getAvailableCount(), need - allocatedThisLine);
                if (take <= 0) {
                    continue;
                }

                // 原子预占锁库
                WmsInventoryDimension dimension = new WmsInventoryDimension()
                        .setWarehouseId(inv.getWarehouseId())
                        .setWarehouseAreaId(inv.getWarehouseAreaId())
                        .setWarehouseAreaLocaId(inv.getWarehouseAreaLocaId())
                        .setConsignorId(inv.getConsignorId())
                        .setSkuId(inv.getSkuId())
                        .setBatchNumber(inv.getBatchNumber())
                        .setQualityStatus(inv.getQualityStatus());

                inventoryService.applyChange(dimension, 0, take, 0,
                        WmsInventoryBizTypeEnum.LOCK, order.getOutboundCode(), "出库单分配预占锁库");

                allocatedThisLine += take;
                currentAllocations.add(new AllocatedRecord(dimension, take));

                // 回写明细首个命中的库位与批次
                if (detail.getTargetWarehouseAreaLocaId() == null || detail.getTargetWarehouseAreaLocaId() == 0) {
                    detail.setTargetWarehouseId(inv.getWarehouseId())
                            .setTargetWarehouseAreaId(inv.getWarehouseAreaId())
                            .setTargetWarehouseAreaLocaId(inv.getWarehouseAreaLocaId())
                            .setBatchNumber(inv.getBatchNumber())
                            .setExpireDate(inv.getExpireDate());
                }

                if (allocatedThisLine >= need) {
                    break;
                }
            }

            int finalAllocated = alreadyAllocated + allocatedThisLine;
            detail.setAllocatedCount(finalAllocated);
            outboundOrderDetailsMapper.updateById(detail);

            int shortage = detail.getOutCount() - finalAllocated;
            lineVo.setAllocatedCount(finalAllocated);
            lineVo.setShortageCount(shortage);
            lineVo.setIsShortage(shortage > 0);
            lineVo.setBatchNumber(detail.getBatchNumber());
            lineVo.setLocaId(detail.getTargetWarehouseAreaLocaId());
            if (detail.getTargetWarehouseAreaLocaId() != null && detail.getTargetWarehouseAreaLocaId() > 0) {
                WmsWarehouseAreaLoca loca = locaMapper.selectById(detail.getTargetWarehouseAreaLocaId());
                lineVo.setLocaCode(loca != null ? loca.getLocaCode() : "");
            }
            lineResults.add(lineVo);

            if (shortage > 0) {
                hasShortage = true;
                if (Objects.equals(detail.getShortageHandle(), WmsShortageHandleEnum.SUSPEND_ORDER.getCode())) {
                    hasSuspendStrategy = true;
                }
            }
        }

        WmsOutboundAllocateResultVo result = new WmsOutboundAllocateResultVo();
        result.setOutboundOrderId(order.getId());
        result.setOutboundCode(order.getOutboundCode());
        result.setLineResults(lineResults);

        // 缺货与挂起逻辑处理
        if (hasShortage && hasSuspendStrategy) {
            // 策略为整单挂起：回滚本轮所有锁库，状态置为 30 待补货
            for (AllocatedRecord rec : currentAllocations) {
                inventoryService.applyChange(rec.dimension, 0, -rec.count, 0,
                        WmsInventoryBizTypeEnum.UNLOCK, order.getOutboundCode(), "分配缺货整单挂起回滚锁库");
            }
            // 明细 allocatedCount 重置
            for (WmsOutboundOrderDetails detail : details) {
                detail.setAllocatedCount(0);
                outboundOrderDetailsMapper.updateById(detail);
            }
            order.setStatus(WmsOutboundStatusEnum.WAIT_REPLENISH.getCode());
            order.setUpdateTime(new Date());
            updateById(order);

            result.setFullyAllocated(false);
            result.setStatus(WmsOutboundStatusEnum.WAIT_REPLENISH.getCode());
            result.setMessage("存在缺货行且缺货处理策略为[整单挂起]，已回滚预占库存，单据挂起待补货");
            return result;
        }

        // 成功分配或剔除缺货行部分分配
        order.setStatus(WmsOutboundStatusEnum.ALLOCATED.getCode());
        order.setUpdateTime(new Date());
        updateById(order);

        result.setFullyAllocated(!hasShortage);
        result.setStatus(WmsOutboundStatusEnum.ALLOCATED.getCode());
        result.setMessage(hasShortage ? "库存部分分配成功，已剔除缺货部分" : "库存全额分配并锁库成功");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPick(WmsPickConfirmForm form) {
        WmsOutboundOrder order = getById(form.getOutboundOrderId());
        if (order == null) {
            throw new JeecgBootException("出库单不存在: id=" + form.getOutboundOrderId());
        }

        if (order.getStatus() != WmsOutboundStatusEnum.ALLOCATED.getCode()
                && order.getStatus() != WmsOutboundStatusEnum.PICKING.getCode()) {
            throw new JeecgBootException("当前出库单状态不允许拣货确认，状态=" + order.getStatus());
        }

        Map<Long, WmsPickConfirmForm.WmsPickLineItem> pickMap = form.getPickLines().stream()
                .collect(Collectors.toMap(WmsPickConfirmForm.WmsPickLineItem::getDetailId, l -> l));

        List<WmsOutboundOrderDetails> details = outboundOrderDetailsMapper.selectList(
                new LambdaQueryWrapper<WmsOutboundOrderDetails>()
                        .eq(WmsOutboundOrderDetails::getOutboundOrderId, order.getId())
                        .eq(WmsOutboundOrderDetails::getIsDel, 1));

        Date now = new Date();
        boolean allPicked = true;

        for (WmsOutboundOrderDetails d : details) {
            WmsPickConfirmForm.WmsPickLineItem pickLine = pickMap.get(d.getId());
            if (pickLine != null) {
                if (pickLine.getPickedCount() > d.getAllocatedCount()) {
                    throw new JeecgBootException("实拣数量[" + pickLine.getPickedCount() + "]不能大于已分配数量[" + d.getAllocatedCount() + "]");
                }

                // SN 序列号管理校验（决策 D2）
                WmsItemSku sku = itemSkuMapper.selectById(d.getSkuId());
                if (sku != null && Objects.equals(sku.getIsSnManaged(), 1)) {
                    List<String> snCodes = pickLine.getSnCodes();
                    if (snCodes == null || snCodes.size() != pickLine.getPickedCount()) {
                        String skuLabel = sku.getTitle() != null ? sku.getTitle() : sku.getCode();
                        throw new JeecgBootException("商品[" + skuLabel + "]启用了SN管理，必须录入与实拣数量相等的SN序列号(应录" + pickLine.getPickedCount() + "个)");
                    }

                    // 校验并锁定 SN
                    for (String snCode : snCodes) {
                        WmsItemSn sn = itemSnMapper.selectOne(new LambdaQueryWrapper<WmsItemSn>()
                                .eq(WmsItemSn::getSnCode, snCode.trim())
                                .eq(WmsItemSn::getIsDel, 1));
                        if (sn == null) {
                            throw new JeecgBootException("SN序列号[" + snCode + "]不存在");
                        }
                        if (sn.getStatus() != 2) { // 2 在库良品
                            throw new JeecgBootException("SN序列号[" + snCode + "]当前状态不是在库良品(当前状态=" + sn.getStatus() + ")");
                        }
                        if (!Objects.equals(sn.getSkuId(), d.getSkuId())) {
                            throw new JeecgBootException("SN序列号[" + snCode + "]与商品SKU不匹配");
                        }

                        // 置为 4 拣货锁定，并关联出库单
                        sn.setStatus(4); // 4 拣货锁定
                        sn.setOutboundOrderId(order.getId());
                        sn.setUpdateTime(now);
                        itemSnMapper.updateById(sn);
                    }
                }

                d.setPickedCount(pickLine.getPickedCount());
                d.setPickStatus(2); // 已拣货
                d.setPickTime(now);
                outboundOrderDetailsMapper.updateById(d);
            }

            if (d.getPickedCount() == null || d.getPickedCount() < d.getAllocatedCount()) {
                allPicked = false;
            }
        }

        // 若全部拣货完成，流转至 50 拣货完成待复核
        if (allPicked) {
            order.setStatus(WmsOutboundStatusEnum.WAIT_REVIEW.getCode());
        } else {
            order.setStatus(WmsOutboundStatusEnum.PICKING.getCode());
        }
        order.setUpdateTime(now);
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReview(WmsReviewConfirmForm form) {
        WmsOutboundOrder order = getById(form.getOutboundOrderId());
        if (order == null) {
            throw new JeecgBootException("出库单不存在: id=" + form.getOutboundOrderId());
        }

        if (order.getStatus() != WmsOutboundStatusEnum.WAIT_REVIEW.getCode()) {
            throw new JeecgBootException("出库单当前不是待复核状态(50)，无法复核");
        }

        Map<Long, WmsReviewConfirmForm.WmsReviewLineItem> reviewMap = form.getReviewLines().stream()
                .collect(Collectors.toMap(WmsReviewConfirmForm.WmsReviewLineItem::getDetailId, r -> r));

        List<WmsOutboundOrderDetails> details = outboundOrderDetailsMapper.selectList(
                new LambdaQueryWrapper<WmsOutboundOrderDetails>()
                        .eq(WmsOutboundOrderDetails::getOutboundOrderId, order.getId())
                        .eq(WmsOutboundOrderDetails::getIsDel, 1));

        boolean hasReject = false;
        for (WmsOutboundOrderDetails d : details) {
            WmsReviewConfirmForm.WmsReviewLineItem reviewItem = reviewMap.get(d.getId());
            if (reviewItem != null) {
                d.setReviewCount(reviewItem.getReviewCount());
                d.setReviewStatus(reviewItem.getReviewStatus());
                outboundOrderDetailsMapper.updateById(d);

                if (Objects.equals(reviewItem.getReviewStatus(), WmsReviewStatusEnum.REJECT.getCode())) {
                    hasReject = true;
                }
            } else {
                hasReject = true;
            }
        }

        Date now = new Date();
        if (hasReject) {
            // 复核不通过，单据进入 92 异常挂起
            order.setStatus(WmsOutboundStatusEnum.EXCEPTION_SUSPEND.getCode());
        } else {
            // 全部复核通过，推进至 60 复核通过待打包
            order.setStatus(WmsOutboundStatusEnum.REVIEWED.getCode());
        }
        order.setUpdateTime(now);
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void packageAndWeigh(WmsPackageWeightForm form) {
        WmsOutboundOrder order = getById(form.getOutboundOrderId());
        if (order == null) {
            throw new JeecgBootException("出库单不存在: id=" + form.getOutboundOrderId());
        }

        if (order.getStatus() != WmsOutboundStatusEnum.REVIEWED.getCode()) {
            throw new JeecgBootException("出库单当前不是复核通过状态(60)，无法打包称重");
        }

        Date now = new Date();
        String datePrefix = new SimpleDateFormat("yyyyMMdd").format(now);

        // 1. 保存打包记录
        if (form.getPackages() != null && !form.getPackages().isEmpty()) {
            for (WmsPackageWeightForm.WmsPackageBoxItem box : form.getPackages()) {
                WmsOutboundOrderPackage pkg = new WmsOutboundOrderPackage();
                pkg.setPackageCode(box.getPackageCode() != null ? box.getPackageCode() : "PKG" + datePrefix + System.currentTimeMillis() % 10000);
                pkg.setOutboundOrderId(order.getId());
                pkg.setOutboundOrderDetailId(box.getDetailId());
                pkg.setSkuId(box.getSkuId());
                pkg.setPackageCount(box.getPackageCount());
                pkg.setPackageTime(now);
                pkg.setCreateTime(now);
                outboundOrderPackageMapper.insert(pkg);
            }
        }

        // 2. 称重记录与理论重量校验
        List<WmsOutboundOrderDetails> details = outboundOrderDetailsMapper.selectList(
                new LambdaQueryWrapper<WmsOutboundOrderDetails>()
                        .eq(WmsOutboundOrderDetails::getOutboundOrderId, order.getId())
                        .eq(WmsOutboundOrderDetails::getIsDel, 1));

        int theoreticalWeight = 0;
        for (WmsOutboundOrderDetails d : details) {
            WmsItemSku sku = itemSkuMapper.selectById(d.getSkuId());
            if (sku != null && sku.getProductWeight() != null) {
                theoreticalWeight += sku.getProductWeight() * (d.getPickedCount() != null ? d.getPickedCount() : 0);
            }
        }

        if (theoreticalWeight > 0) {
            double diffRatio = Math.abs((double) (form.getActualWeight() - theoreticalWeight)) / theoreticalWeight;
            if (diffRatio > 0.1) {
                log.warn("出库单[{}]称重超差: 理论重量={}g, 实际称重={}g, 偏差率={}%",
                        order.getOutboundCode(), theoreticalWeight, form.getActualWeight(), String.format("%.2f", diffRatio * 100));
            }
        }

        WmsOutboundOrderWeights weights = new WmsOutboundOrderWeights();
        weights.setWeightsCode("WT" + datePrefix + System.currentTimeMillis() % 10000);
        weights.setOutboundOrderId(order.getId());
        weights.setOutboundOrderDetailId(details.get(0).getId());
        weights.setWeightsEmpId(0);
        weights.setWeightsTime(now);
        weights.setWeightVal(form.getActualWeight());
        weights.setWeightsUnit(1); // 1克
        weights.setCreateTime(now);
        outboundOrderWeightsMapper.insert(weights);

        // 3. 更新单头状态为 70 已打包待发货
        order.setStatus(WmsOutboundStatusEnum.PACKED.getCode());
        order.setTotalWeight(form.getActualWeight());
        if (form.getLogisticsCompanyId() != null) {
            order.setLogisticsCompanyId(form.getLogisticsCompanyId());
        }
        if (form.getLogisticsNumber() != null && !form.getLogisticsNumber().trim().isEmpty()) {
            order.setLogisticsNumber(form.getLogisticsNumber().trim());
        }
        order.setUpdateTime(now);
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmShip(WmsShipConfirmForm form) {
        WmsOutboundOrder order = getById(form.getOutboundOrderId());
        if (order == null) {
            throw new JeecgBootException("出库单不存在: id=" + form.getOutboundOrderId());
        }

        if (order.getStatus() != WmsOutboundStatusEnum.PACKED.getCode()
                && order.getStatus() != WmsOutboundStatusEnum.REVIEWED.getCode()) {
            throw new JeecgBootException("出库单必须在已打包或已复核状态才能执行发运出库，当前状态=" + order.getStatus());
        }

        List<WmsOutboundOrderDetails> details = outboundOrderDetailsMapper.selectList(
                new LambdaQueryWrapper<WmsOutboundOrderDetails>()
                        .eq(WmsOutboundOrderDetails::getOutboundOrderId, order.getId())
                        .eq(WmsOutboundOrderDetails::getIsDel, 1));

        Date now = new Date();

        // 1. 扣减库存：total_count 减少，locked_count 减少，写流水（业务类型 2 出库扣减）
        for (WmsOutboundOrderDetails d : details) {
            int shipCount = d.getPickedCount() != null ? d.getPickedCount() : d.getAllocatedCount();
            if (shipCount <= 0) {
                continue;
            }

            WmsInventoryDimension dimension = new WmsInventoryDimension()
                    .setWarehouseId(order.getWarehouseId())
                    .setWarehouseAreaId(d.getTargetWarehouseAreaId())
                    .setWarehouseAreaLocaId(d.getTargetWarehouseAreaLocaId())
                    .setConsignorId(order.getConsignorId())
                    .setSkuId(d.getSkuId())
                    .setBatchNumber(d.getBatchNumber() != null ? d.getBatchNumber() : "")
                    .setQualityStatus(1); // 良品

            inventoryService.applyChange(dimension, -shipCount, -shipCount, 0,
                    WmsInventoryBizTypeEnum.OUTBOUND_DEDUCT, order.getOutboundCode(), "出库单确认发货扣减库存");
        }

        // 2. 更新 SN 状态为 5 已出库
        List<WmsItemSn> snList = itemSnMapper.selectList(
                new LambdaQueryWrapper<WmsItemSn>()
                        .eq(WmsItemSn::getOutboundOrderId, order.getId())
                        .eq(WmsItemSn::getStatus, 4) // 4 拣货锁定
                        .eq(WmsItemSn::getIsDel, 1));

        for (WmsItemSn sn : snList) {
            sn.setStatus(5); // 5 已出库
            sn.setUpdateTime(now);
            itemSnMapper.updateById(sn);
        }

        // 3. 更新出库单状态为 80 已发货
        if (form.getLogisticsCompanyId() != null) {
            order.setLogisticsCompanyId(form.getLogisticsCompanyId());
        }
        if (form.getLogisticsNumber() != null && !form.getLogisticsNumber().trim().isEmpty()) {
            order.setLogisticsNumber(form.getLogisticsNumber().trim());
        }
        order.setStatus(WmsOutboundStatusEnum.SHIPPED.getCode());
        order.setUpdateTime(now);
        updateById(order);

        log.info("出库单[{}]已发货确认，库存扣减完毕，SN追溯登记完成", order.getOutboundCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOutboundOrder(Long orderId, String reason) {
        WmsOutboundOrder order = getById(orderId);
        if (order == null) {
            throw new JeecgBootException("出库单不存在: id=" + orderId);
        }

        if (order.getStatus() >= WmsOutboundStatusEnum.SHIPPED.getCode()) {
            throw new JeecgBootException("出库单已发运出库（状态=" + order.getStatus() + "），禁止直接取消！如需退回请走逆向退货入库流程。");
        }

        List<WmsOutboundOrderDetails> details = outboundOrderDetailsMapper.selectList(
                new LambdaQueryWrapper<WmsOutboundOrderDetails>()
                        .eq(WmsOutboundOrderDetails::getOutboundOrderId, order.getId())
                        .eq(WmsOutboundOrderDetails::getIsDel, 1));

        Date now = new Date();

        // 1. 未拣货阶段（状态 <= 20）：直接释放锁库数量
        if (order.getStatus() <= WmsOutboundStatusEnum.ALLOCATED.getCode()) {
            for (WmsOutboundOrderDetails d : details) {
                if (d.getAllocatedCount() != null && d.getAllocatedCount() > 0) {
                    WmsInventoryDimension dimension = new WmsInventoryDimension()
                            .setWarehouseId(order.getWarehouseId())
                            .setWarehouseAreaId(d.getTargetWarehouseAreaId())
                            .setWarehouseAreaLocaId(d.getTargetWarehouseAreaLocaId())
                            .setConsignorId(order.getConsignorId())
                            .setSkuId(d.getSkuId())
                            .setBatchNumber(d.getBatchNumber())
                            .setQualityStatus(1);

                    inventoryService.applyChange(dimension, 0, -d.getAllocatedCount(), 0,
                            WmsInventoryBizTypeEnum.UNLOCK, order.getOutboundCode(), "取消出库单释放锁库");

                    d.setAllocatedCount(0);
                    outboundOrderDetailsMapper.updateById(d);
                }
            }
        } else {
            // 2. 已拣货但未发货阶段（状态 40~70）：释放锁库 + 释放SN + 生成取消返架单
            for (WmsOutboundOrderDetails d : details) {
                if (d.getAllocatedCount() != null && d.getAllocatedCount() > 0) {
                    WmsInventoryDimension dimension = new WmsInventoryDimension()
                            .setWarehouseId(order.getWarehouseId())
                            .setWarehouseAreaId(d.getTargetWarehouseAreaId())
                            .setWarehouseAreaLocaId(d.getTargetWarehouseAreaLocaId())
                            .setConsignorId(order.getConsignorId())
                            .setSkuId(d.getSkuId())
                            .setBatchNumber(d.getBatchNumber())
                            .setQualityStatus(1);

                    inventoryService.applyChange(dimension, 0, -d.getAllocatedCount(), 0,
                            WmsInventoryBizTypeEnum.UNLOCK, order.getOutboundCode(), "取消出库单返架释放锁库");
                }
            }

            // 恢复 SN 序列号为 2 在库良品
            List<WmsItemSn> snList = itemSnMapper.selectList(
                    new LambdaQueryWrapper<WmsItemSn>()
                            .eq(WmsItemSn::getOutboundOrderId, order.getId())
                            .eq(WmsItemSn::getStatus, 4)
                            .eq(WmsItemSn::getIsDel, 1));

            for (WmsItemSn sn : snList) {
                sn.setStatus(2); // 2 在库良品
                sn.setOutboundOrderId(null);
                sn.setUpdateTime(now);
                itemSnMapper.updateById(sn);
            }

            // 生成「取消返架单」（wms_warehouse_operations，operation_type = 4 取消返架）
            String datePrefix = new SimpleDateFormat("yyyyMMdd").format(now);
            WmsWarehouseOperations op = new WmsWarehouseOperations();
            op.setJobCode("RET" + datePrefix + System.currentTimeMillis() % 10000)
                    .setWarehouseId(order.getWarehouseId())
                    .setCategory(1)
                    .setOperationType(4) // 4 取消返架
                    .setSourceType(1)
                    .setStatus(WmsOperationStatusEnum.PENDING.getCode())
                    .setRemark("取消出库单[" + order.getOutboundCode() + "]触发返架: " + (reason != null ? reason : ""))
                    .setCreateTime(now)
                    .setIsDel(1);

            operationsMapper.insert(op);

            for (WmsOutboundOrderDetails d : details) {
                int returnCount = d.getPickedCount() != null && d.getPickedCount() > 0 ? d.getPickedCount() : d.getAllocatedCount();
                if (returnCount > 0) {
                    WmsWarehouseOperationsDetails opDetail = new WmsWarehouseOperationsDetails();
                    opDetail.setWarehouseOperationsId(op.getId())
                            .setSkuId(d.getSkuId())
                            .setBatchNumber(d.getBatchNumber())
                            .setConsignorId(order.getConsignorId())
                            .setQualityStatus(1)
                            .setSourceWarehouseAreaId(d.getTargetWarehouseAreaId())
                            .setSourceWarehouseAreaLocaId(d.getTargetWarehouseAreaLocaId())
                            .setTargetWarehouseId(order.getWarehouseId())
                            .setTargetWarehouseAreaId(d.getTargetWarehouseAreaId())
                            .setTargetWarehouseAreaLocaId(d.getTargetWarehouseAreaLocaId())
                            .setSourceCount(returnCount)
                            .setTargetCount(returnCount);
                    operationsDetailsMapper.insert(opDetail);
                }
            }
            log.info("出库单[{}]已拣货取消，生成返架作业单[{}]", order.getOutboundCode(), op.getJobCode());
        }

        // 更新状态为 91 已取消
        order.setStatus(WmsOutboundStatusEnum.CANCELLED.getCode());
        order.setUpdateTime(now);
        updateById(order);
    }

    private String generateOutboundCode(String datePrefix) {
        String base = "OB" + datePrefix;
        int seq = 1;
        String full = base + String.format("%04d", seq);
        while (baseMapper.selectByCode(full) != null) {
            seq++;
            full = base + String.format("%04d", seq);
        }
        return full;
    }

    private static class AllocatedRecord {
        final WmsInventoryDimension dimension;
        final int count;

        AllocatedRecord(WmsInventoryDimension dimension, int count) {
            this.dimension = dimension;
            this.count = count;
        }
    }
}
