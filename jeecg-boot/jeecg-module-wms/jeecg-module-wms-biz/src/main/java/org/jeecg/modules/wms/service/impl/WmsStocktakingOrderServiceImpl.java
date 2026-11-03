package org.jeecg.modules.wms.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsStocktakingDetail;
import org.jeecg.modules.wms.entity.WmsStocktakingLoss;
import org.jeecg.modules.wms.entity.WmsStocktakingOrder;
import org.jeecg.modules.wms.entity.WmsOutboundOrder;
import org.jeecg.modules.wms.entity.WmsOutboundOrderDetails;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.entity.WmsWarehousingEntry;
import org.jeecg.modules.wms.entity.WmsWarehousingEntryDetails;
import org.jeecg.modules.wms.enums.WmsInventoryBizTypeEnum;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.mapper.WmsOutboundOrderDetailsMapper;
import org.jeecg.modules.wms.mapper.WmsOutboundOrderMapper;
import org.jeecg.modules.wms.mapper.WmsStocktakingDetailMapper;
import org.jeecg.modules.wms.mapper.WmsStocktakingLossMapper;
import org.jeecg.modules.wms.mapper.WmsStocktakingOrderMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryDetailsMapper;
import org.jeecg.modules.wms.mapper.WmsWarehousingEntryMapper;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.service.IWmsStocktakingOrderService;
import org.jeecg.modules.wms.vo.WmsInventoryDimension;
import org.jeecg.modules.wms.vo.WmsStocktakingOrderForm;
import org.jeecg.modules.wms.vo.WmsStocktakingSkuSelectVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Description: 盘库单服务实现类
 * @Author: jeecg-boot
 * @Date: 2026-11-03
 * @Version: V1.0
 */
@Slf4j
@Service
public class WmsStocktakingOrderServiceImpl extends ServiceImpl<WmsStocktakingOrderMapper, WmsStocktakingOrder>
        implements IWmsStocktakingOrderService {

    /** 损溢类型：盘亏 */
    private static final int LOSS_TYPE_SHORTAGE = 1;
    /** 损溢类型：盘盈 */
    private static final int LOSS_TYPE_SURPLUS = 2;
    /** 损溢原因：盘点差异 */
    private static final int LOSS_CATEGORY_STOCKTAKING = 1;

    @Autowired
    private WmsStocktakingOrderMapper stocktakingOrderMapper;

    @Autowired
    private WmsStocktakingDetailMapper stocktakingDetailMapper;

    @Autowired
    private WmsInventoryMapper inventoryMapper;

    @Autowired
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Autowired
    private IWmsInventoryService inventoryService;

    @Autowired
    private WmsStocktakingLossMapper lossMapper;

    @Autowired
    private WmsWarehousingEntryMapper warehousingEntryMapper;

    @Autowired
    private WmsWarehousingEntryDetailsMapper warehousingEntryDetailsMapper;

    @Autowired
    private WmsOutboundOrderMapper outboundOrderMapper;

    @Autowired
    private WmsOutboundOrderDetailsMapper outboundOrderDetailsMapper;

    @Override
    public IPage<WmsStocktakingOrder> queryPageList(Page<WmsStocktakingOrder> page,
                                                   String orderNo,
                                                   Integer warehouseId,
                                                   Integer status,
                                                   String startDate,
                                                   String endDate) {
        LambdaQueryWrapper<WmsStocktakingOrder> query = new LambdaQueryWrapper<WmsStocktakingOrder>()
                .orderByDesc(WmsStocktakingOrder::getId);

        if (oConvertUtils.isNotEmpty(orderNo)) {
            query.like(WmsStocktakingOrder::getOrderNo, orderNo.trim());
        }
        if (warehouseId != null) {
            query.eq(WmsStocktakingOrder::getWarehouseId, warehouseId);
        }
        if (status != null) {
            query.eq(WmsStocktakingOrder::getStatus, status);
        }
        if (oConvertUtils.isNotEmpty(startDate)) {
            query.ge(WmsStocktakingOrder::getOrderDate, startDate);
        }
        if (oConvertUtils.isNotEmpty(endDate)) {
            query.le(WmsStocktakingOrder::getOrderDate, endDate);
        }
        return this.page(page, query);
    }

    @Override
    public WmsStocktakingOrderForm queryDetailById(Long id) {
        WmsStocktakingOrder order = this.getById(id);
        if (order == null) {
            throw new JeecgBootException("盘库单不存在");
        }
        List<WmsStocktakingDetail> details = stocktakingDetailMapper.selectByOrderId(id);
        if (details != null) {
            details.forEach(d -> d.setSkuName(formatSpecName(d.getSkuName())));
        }

        WmsStocktakingOrderForm form = new WmsStocktakingOrderForm();
        BeanUtils.copyProperties(order, form);
        form.setDetails(details);
        return form;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsStocktakingOrder saveDraft(WmsStocktakingOrderForm form) {
        validateBasicForm(form);
        return doSaveOrUpdateOrder(form, STATUS_DRAFT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsStocktakingOrder complete(WmsStocktakingOrderForm form) {
        validateBasicForm(form);
        List<WmsStocktakingDetail> details = form.getDetails();
        if (details == null || details.isEmpty()) {
            throw new JeecgBootException("盘库商品明细不能为空，请添加盘库商品！");
        }

        // 校验每一行明细的实际库存
        for (int i = 0; i < details.size(); i++) {
            WmsStocktakingDetail detail = details.get(i);
            if (detail.getActualCount() == null || detail.getActualCount() < 0) {
                String code = oConvertUtils.isNotEmpty(detail.getSkuCode()) ? detail.getSkuCode() : ("第 " + (i + 1) + " 行");
                throw new JeecgBootException("商品【" + code + "】的实际库存必须填写且不能小于 0！");
            }
        }

        // 保存/更新盘库单主子表信息（状态置为已盘点，解耦物理库存调账，仅锁定数据等待台账手动触发调账）
        return doSaveOrUpdateOrder(form, STATUS_COUNTED);
    }

    @Override
    public IPage<WmsStocktakingSkuSelectVO> selectSkuPageForStocktaking(Page<WmsStocktakingSkuSelectVO> page,
                                                                      Integer categoryId,
                                                                      String skuCode,
                                                                      String skuName,
                                                                      String spuCode,
                                                                      String spuName,
                                                                      Integer warehouseId) {
        IPage<WmsStocktakingSkuSelectVO> resultPage = stocktakingDetailMapper.selectSkuPageForStocktaking(page, categoryId, skuCode, skuName, spuCode, spuName, warehouseId);
        if (resultPage != null && resultPage.getRecords() != null) {
            resultPage.getRecords().forEach(this::cleanSkuVoSpecInfo);
        }
        return resultPage;
    }

    @Override
    public List<WmsStocktakingSkuSelectVO> importWarehouseAllSkus(Integer warehouseId) {
        if (warehouseId == null) {
            throw new JeecgBootException("请先指定盘点仓库！");
        }
        List<WmsStocktakingSkuSelectVO> list = stocktakingDetailMapper.selectAllSkusInWarehouse(warehouseId);
        if (list != null) {
            list.forEach(this::cleanSkuVoSpecInfo);
        }
        return list;
    }

    /**
     * 清洗 SKU 规格 JSON，提取人类可读的规格名与单位
     */
    private void cleanSkuVoSpecInfo(WmsStocktakingSkuSelectVO vo) {
        if (vo == null) {
            return;
        }
        vo.setSkuName(formatSpecName(vo.getSkuName()));
    }

    /**
     * 将 spec 原始 JSON 解析为清晰规格名
     */
    private String formatSpecName(String spec) {
        if (oConvertUtils.isEmpty(spec)) {
            return "";
        }
        String trimmed = spec.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return trimmed;
        }
        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(trimmed);
            if (json != null) {
                if (json.containsKey("specName") && oConvertUtils.isNotEmpty(json.getString("specName"))) {
                    return json.getString("specName");
                }
                List<String> values = new ArrayList<>();
                for (String key : json.keySet()) {
                    if (!"specCode".equalsIgnoreCase(key) && !"unit".equalsIgnoreCase(key) && !"code".equalsIgnoreCase(key)) {
                        String val = json.getString(key);
                        if (oConvertUtils.isNotEmpty(val)) {
                            values.add(val);
                        }
                    }
                }
                if (!values.isEmpty()) {
                    return String.join(" ", values);
                }
            }
        } catch (Exception ignored) {
        }
        return trimmed;
    }

    @Override
    public Long getSkuBookStock(Integer warehouseId, Long skuId) {
        if (warehouseId == null || skuId == null) {
            return 0L;
        }
        return inventoryMapper.sumTotalCount(warehouseId, skuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        WmsStocktakingOrder order = this.getById(id);
        if (order == null) {
            throw new JeecgBootException("盘库单不存在！");
        }
        if (order.getStatus() != null && order.getStatus() == STATUS_ADJUSTED) {
            throw new JeecgBootException("已完成调账的盘库单不能删除！");
        }
        // 软删除主表
        this.removeById(id);
        // 软删除子表
        stocktakingDetailMapper.delete(new LambdaQueryWrapper<WmsStocktakingDetail>()
                .eq(WmsStocktakingDetail::getOrderId, id));
    }

    /**
     * 基础表单校验
     */
    private void validateBasicForm(WmsStocktakingOrderForm form) {
        if (form == null) {
            throw new JeecgBootException("提交数据不能为空");
        }
        if (form.getWarehouseId() == null) {
            throw new JeecgBootException("请选择盘库仓库！");
        }
        if (form.getOrderDate() == null) {
            throw new JeecgBootException("请选择单据日期！");
        }
    }

    /**
     * 统一保存/更新主子表逻辑
     */
    private WmsStocktakingOrder doSaveOrUpdateOrder(WmsStocktakingOrderForm form, int targetStatus) {
        Date now = new Date();
        String orderNo = form.getOrderNo();
        if (oConvertUtils.isEmpty(orderNo)) {
            orderNo = generateOrderNo();
        }

        // 计算各项合计
        int totalBookCount = 0;
        int totalActualCount = 0;
        int totalDiscrepancyCount = 0;
        int profitCount = 0;
        BigDecimal profitAmount = BigDecimal.ZERO;
        int lossCount = 0;
        BigDecimal lossAmount = BigDecimal.ZERO;
        BigDecimal totalBookAmount = BigDecimal.ZERO;
        BigDecimal totalActualAmount = BigDecimal.ZERO;
        BigDecimal totalDiscrepancyAmount = BigDecimal.ZERO;

        List<WmsStocktakingDetail> details = form.getDetails();
        if (details != null && !details.isEmpty()) {
            for (WmsStocktakingDetail d : details) {
                int book = d.getBookCount() != null ? d.getBookCount() : 0;
                totalBookCount += book;

                BigDecimal price = d.getUnitPrice() != null ? d.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal bookAmt = price.multiply(BigDecimal.valueOf(book)).setScale(2, RoundingMode.HALF_UP);
                totalBookAmount = totalBookAmount.add(bookAmt);

                if (d.getActualCount() != null) {
                    int actual = d.getActualCount();
                    totalActualCount += actual;

                    BigDecimal actualAmt = price.multiply(BigDecimal.valueOf(actual)).setScale(2, RoundingMode.HALF_UP);
                    d.setActualAmount(actualAmt);
                    totalActualAmount = totalActualAmount.add(actualAmt);

                    int discCount = actual - book;
                    d.setDiscrepancyCount(discCount);
                    totalDiscrepancyCount += discCount;

                    BigDecimal discAmt = price.multiply(BigDecimal.valueOf(discCount)).setScale(2, RoundingMode.HALF_UP);
                    d.setDiscrepancyAmount(discAmt);
                    totalDiscrepancyAmount = totalDiscrepancyAmount.add(discAmt);

                    if (discCount > 0) {
                        profitCount += discCount;
                        profitAmount = profitAmount.add(discAmt);
                    } else if (discCount < 0) {
                        lossCount += Math.abs(discCount);
                        lossAmount = lossAmount.add(price.multiply(BigDecimal.valueOf(Math.abs(discCount))).setScale(2, RoundingMode.HALF_UP));
                    }
                } else {
                    d.setActualAmount(BigDecimal.ZERO);
                    d.setDiscrepancyCount(0);
                    d.setDiscrepancyAmount(BigDecimal.ZERO);
                }
            }
        }

        // 判断盘库状态及出入库调整状态
        int realStatus = targetStatus;
        int adjustInStatus = ADJUST_STATUS_PENDING;
        int adjustOutStatus = ADJUST_STATUS_PENDING;

        if (targetStatus == STATUS_DRAFT) {
            adjustInStatus = 0;
            adjustOutStatus = 0;
        } else {
            // 完成盘点模式：无盈亏直接进入已调账
            if (profitCount == 0 && lossCount == 0) {
                realStatus = STATUS_ADJUSTED;
                adjustInStatus = ADJUST_STATUS_NONE;
                adjustOutStatus = ADJUST_STATUS_NONE;
            } else {
                realStatus = STATUS_COUNTED;
                adjustInStatus = profitCount > 0 ? ADJUST_STATUS_PENDING : ADJUST_STATUS_NONE;
                adjustOutStatus = lossCount > 0 ? ADJUST_STATUS_PENDING : ADJUST_STATUS_NONE;
            }
        }

        WmsStocktakingOrder order = new WmsStocktakingOrder();
        if (form.getId() != null) {
            order = this.getById(form.getId());
            if (order == null) {
                throw new JeecgBootException("盘库单不存在，ID: " + form.getId());
            }
            if (order.getStatus() != null && order.getStatus() == STATUS_ADJUSTED) {
                throw new JeecgBootException("已完成调账的盘库单不可再次修改！");
            }
        }

        order.setOrderNo(orderNo)
                .setWarehouseId(form.getWarehouseId())
                .setOrderDate(form.getOrderDate())
                .setTotalBookCount(totalBookCount)
                .setTotalActualCount(totalActualCount)
                .setTotalDiscrepancyCount(totalDiscrepancyCount)
                .setTotalBookAmount(totalBookAmount)
                .setTotalActualAmount(totalActualAmount)
                .setTotalDiscrepancyAmount(totalDiscrepancyAmount)
                .setProfitCount(profitCount)
                .setProfitAmount(profitAmount)
                .setLossCount(lossCount)
                .setLossAmount(lossAmount)
                .setRemark(form.getRemark())
                .setStatus(realStatus)
                .setAdjustInStatus(adjustInStatus)
                .setAdjustOutStatus(adjustOutStatus)
                .setApprovalSwitch(form.getApprovalSwitch() != null ? form.getApprovalSwitch() : 0)
                .setIsDel(1);

        if (form.getId() == null) {
            order.setCreateTime(now);
            this.save(order);
        } else {
            order.setUpdateTime(now);
            this.updateById(order);
            // 清理旧明细
            stocktakingDetailMapper.delete(new LambdaQueryWrapper<WmsStocktakingDetail>()
                    .eq(WmsStocktakingDetail::getOrderId, order.getId()));
        }

        // 保存新明细
        if (details != null && !details.isEmpty()) {
            for (WmsStocktakingDetail d : details) {
                d.setId(null);
                d.setOrderId(order.getId());
                d.setOrderNo(order.getOrderNo());
                d.setSkuName(formatSpecName(d.getSkuName()));
                d.setIsDel(1);
                d.setCreateTime(now);
                stocktakingDetailMapper.insert(d);
            }
        }

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createProfitEntry(Long orderId) {
        if (orderId == null) {
            throw new JeecgBootException("盘库单ID不能为空");
        }
        WmsStocktakingOrder order = this.getById(orderId);
        if (order == null) {
            throw new JeecgBootException("盘库单不存在");
        }
        if (order.getStatus() == STATUS_DRAFT) {
            throw new JeecgBootException("草稿状态的盘库单不能生成盘盈入库，请先完成盘点！");
        }
        if (order.getAdjustInStatus() != null && order.getAdjustInStatus() == ADJUST_STATUS_DONE) {
            throw new JeecgBootException("该盘库单已生成盘盈入库单，单号: " + order.getAdjustInOrderNo());
        }

        List<WmsStocktakingDetail> details = stocktakingDetailMapper.selectByOrderId(orderId);
        List<WmsStocktakingDetail> profitDetails = details.stream()
                .filter(d -> d.getDiscrepancyCount() != null && d.getDiscrepancyCount() > 0)
                .toList();

        if (profitDetails.isEmpty()) {
            order.setAdjustInStatus(ADJUST_STATUS_NONE);
            this.updateById(order);
            throw new JeecgBootException("该盘库单没有盘盈商品，无需生成盘盈入库！");
        }

        Date now = new Date();
        String rkCode = generateRkCode();
        int totalProfitCount = profitDetails.stream().mapToInt(WmsStocktakingDetail::getDiscrepancyCount).sum();

        // 1. 插入入库单主表
        WmsWarehousingEntry entry = new WmsWarehousingEntry()
                .setEntryCode(rkCode)
                .setEntryType(4) // 4其他入库(盘盈入库)
                .setWarehouseId(order.getWarehouseId())
                .setConsignorId(0L)
                .setBatchNumber(order.getOrderNo())
                .setErpPoNo("盘库单 " + order.getOrderNo() + " 盘盈入库调整")
                .setTotalCount(totalProfitCount)
                .setPredictedArrivalTime(now)
                .setDamageCount(0)
                .setSurpassCount(0)
                .setOverReceiveAuditStatus(0)
                .setStatus(60) // 60已完成
                .setLackCount(0)
                .setWaitSupplementFlag(0)
                .setIsDel(1)
                .setCreateTime(now);
        warehousingEntryMapper.insert(entry);

        // 2. 插入入库单明细并执行物理库存调账
        for (WmsStocktakingDetail detail : profitDetails) {
            int discrepancy = detail.getDiscrepancyCount();
            Long skuId = detail.getSkuId();
            WmsWarehouseAreaLoca targetLoca = findTargetLocaForSku(order.getWarehouseId(), skuId);
            if (targetLoca == null) {
                throw new JeecgBootException("仓库中没有可用库位，无法完成盘盈入库！");
            }

            int priceCents = detail.getUnitPrice() != null
                    ? detail.getUnitPrice().multiply(BigDecimal.valueOf(100)).intValue() : 0;
            WmsWarehousingEntryDetails detailEntry = new WmsWarehousingEntryDetails()
                    .setWarehousingEntryId(entry.getId())
                    .setSkuId(skuId)
                    .setCount(discrepancy)
                    .setReceivedCount(discrepancy)
                    .setDamageCount(0)
                    .setSupplierId(0L)
                    .setPrice(priceCents)
                    .setPredictedArrivalTime(now)
                    .setStatus(1)
                    .setQcStatus(1)
                    .setQcPassCount(discrepancy)
                    .setQcFailCount(0)
                    .setIsDel(1)
                    .setCreateTime(now);
            warehousingEntryDetailsMapper.insert(detailEntry);

            // 执行增库存调账
            WmsInventoryDimension dim = new WmsInventoryDimension()
                    .setWarehouseId(order.getWarehouseId())
                    .setWarehouseAreaId(targetLoca.getWarehouseAreaId())
                    .setWarehouseAreaLocaId(targetLoca.getId())
                    .setSkuId(skuId)
                    .setConsignorId(0L)
                    .setBatchNumber("")
                    .setQualityStatus(1);

            WmsInventory current = inventoryMapper.selectOne(new LambdaQueryWrapper<WmsInventory>()
                    .eq(WmsInventory::getWarehouseAreaLocaId, targetLoca.getId())
                    .eq(WmsInventory::getSkuId, skuId)
                    .eq(WmsInventory::getBatchNumber, "")
                    .eq(WmsInventory::getConsignorId, 0L)
                    .eq(WmsInventory::getQualityStatus, 1));

            int currentTotal = (current != null && current.getTotalCount() != null) ? current.getTotalCount() : 0;
            inventoryService.adjustToTotal(dim, currentTotal + discrepancy, WmsInventoryBizTypeEnum.STOCKTAKING_ADJUST, order.getOrderNo(),
                    "盘库单 " + order.getOrderNo() + " 盘盈入库调整(单号 " + rkCode + "): +" + discrepancy);

            // 损溢记录
            WmsStocktakingLoss loss = new WmsStocktakingLoss()
                    .setLossCount(discrepancy)
                    .setLossCategory(LOSS_CATEGORY_STOCKTAKING)
                    .setLossType(LOSS_TYPE_SURPLUS)
                    .setStocktakingInfo(order.getId())
                    .setBatchNumber(order.getOrderNo())
                    .setCreateTime(now)
                    .setIsDel(1);
            lossMapper.insert(loss);
        }

        // 3. 回写盘库单
        order.setAdjustInStatus(ADJUST_STATUS_DONE);
        order.setAdjustInOrderNo(rkCode);
        if (order.getAdjustOutStatus() != null && order.getAdjustOutStatus() != ADJUST_STATUS_PENDING) {
            order.setStatus(STATUS_ADJUSTED);
        }
        this.updateById(order);

        log.info("盘库单 {} 成功生成盘盈入库单: {}, 盘盈数量: {}", order.getOrderNo(), rkCode, totalProfitCount);
        return rkCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createLossOutbound(Long orderId) {
        if (orderId == null) {
            throw new JeecgBootException("盘库单ID不能为空");
        }
        WmsStocktakingOrder order = this.getById(orderId);
        if (order == null) {
            throw new JeecgBootException("盘库单不存在");
        }
        if (order.getStatus() == STATUS_DRAFT) {
            throw new JeecgBootException("草稿状态的盘库单不能生成盘亏出库，请先完成盘点！");
        }
        if (order.getAdjustOutStatus() != null && order.getAdjustOutStatus() == ADJUST_STATUS_DONE) {
            throw new JeecgBootException("该盘库单已生成盘亏出库单，单号: " + order.getAdjustOutOrderNo());
        }

        List<WmsStocktakingDetail> details = stocktakingDetailMapper.selectByOrderId(orderId);
        List<WmsStocktakingDetail> lossDetails = details.stream()
                .filter(d -> d.getDiscrepancyCount() != null && d.getDiscrepancyCount() < 0)
                .toList();

        if (lossDetails.isEmpty()) {
            order.setAdjustOutStatus(ADJUST_STATUS_NONE);
            this.updateById(order);
            throw new JeecgBootException("该盘库单没有盘亏商品，无需生成盘亏出库！");
        }

        Date now = new Date();
        String ckCode = generateCkCode();
        int totalLossCount = lossDetails.stream().mapToInt(d -> Math.abs(d.getDiscrepancyCount())).sum();

        // 1. 插入出库单主表
        WmsOutboundOrder outbound = new WmsOutboundOrder()
                .setOutboundCode(ckCode)
                .setOutboundType(4) // 4其他出库(盘亏出库)
                .setCustomerId(0L) // 非空
                .setWarehouseId(order.getWarehouseId())
                .setConsignorId(0L)
                .setErpOrderNo("盘库单 " + order.getOrderNo() + " 盘亏出库调整")
                .setTotalCount(totalLossCount)
                .setTotalWeight(0)
                .setTotalVolume(0)
                .setPrice(0)
                .setStatus(80) // 80已出库
                .setIsDel(1)
                .setCreateTime(now);
        outboundOrderMapper.insert(outbound);

        // 2. 插入出库单明细并执行物理库存扣减调账
        for (WmsStocktakingDetail detail : lossDetails) {
            int shortage = Math.abs(detail.getDiscrepancyCount());
            Long skuId = detail.getSkuId();
            WmsWarehouseAreaLoca targetLoca = findTargetLocaForSku(order.getWarehouseId(), skuId);
            if (targetLoca == null) {
                throw new JeecgBootException("仓库中没有可用库位，无法完成盘亏出库！");
            }

            WmsOutboundOrderDetails detailOutbound = new WmsOutboundOrderDetails()
                    .setOutboundOrderId(outbound.getId())
                    .setSkuId(skuId)
                    .setConsignorId(0L)
                    .setBatchNumber("")
                    .setOutCount(shortage)
                    .setAllocatedCount(shortage)
                    .setPickedCount(shortage)
                    .setReviewCount(shortage)
                    .setReviewStatus(1) // 1复核通过
                    .setShortageHandle(1)
                    .setTargetWarehouseId(order.getWarehouseId())
                    .setTargetWarehouseAreaId(targetLoca.getWarehouseAreaId())
                    .setTargetWarehouseAreaLocaId(targetLoca.getId())
                    .setPickStatus(2) // 2已拣货
                    .setIsDel(1)
                    .setCreateTime(now);
            outboundOrderDetailsMapper.insert(detailOutbound);

            // 执行减库存调账（逐行扣减）
            int remainingToDeduct = shortage;
            List<WmsInventory> invRows = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                    .eq(WmsInventory::getWarehouseId, order.getWarehouseId())
                    .eq(WmsInventory::getSkuId, skuId)
                    .eq(WmsInventory::getIsDel, 1)
                    .gt(WmsInventory::getTotalCount, 0)
                    .orderByAsc(WmsInventory::getId));

            for (WmsInventory row : invRows) {
                if (remainingToDeduct <= 0) break;
                int rowTotal = row.getTotalCount() != null ? row.getTotalCount() : 0;
                int deduct = Math.min(rowTotal, remainingToDeduct);

                WmsInventoryDimension dim = new WmsInventoryDimension()
                        .setWarehouseId(row.getWarehouseId())
                        .setWarehouseAreaId(row.getWarehouseAreaId())
                        .setWarehouseAreaLocaId(row.getWarehouseAreaLocaId())
                        .setConsignorId(row.getConsignorId())
                        .setSkuId(row.getSkuId())
                        .setBatchNumber(row.getBatchNumber())
                        .setQualityStatus(row.getQualityStatus());

                inventoryService.adjustToTotal(dim, rowTotal - deduct, WmsInventoryBizTypeEnum.STOCKTAKING_ADJUST, order.getOrderNo(),
                        "盘库单 " + order.getOrderNo() + " 盘亏出库调整(单号 " + ckCode + "): -" + deduct);

                remainingToDeduct -= deduct;
            }

            if (remainingToDeduct > 0) {
                WmsInventoryDimension dim = new WmsInventoryDimension()
                        .setWarehouseId(order.getWarehouseId())
                        .setWarehouseAreaId(targetLoca.getWarehouseAreaId())
                        .setWarehouseAreaLocaId(targetLoca.getId())
                        .setSkuId(skuId)
                        .setConsignorId(0L)
                        .setBatchNumber("")
                        .setQualityStatus(1);
                WmsInventory current = inventoryMapper.selectOne(new LambdaQueryWrapper<WmsInventory>()
                        .eq(WmsInventory::getWarehouseAreaLocaId, targetLoca.getId())
                        .eq(WmsInventory::getSkuId, skuId)
                        .eq(WmsInventory::getBatchNumber, "")
                        .eq(WmsInventory::getConsignorId, 0L)
                        .eq(WmsInventory::getQualityStatus, 1));
                int currentTotal = (current != null && current.getTotalCount() != null) ? current.getTotalCount() : 0;
                int newTotal = Math.max(0, currentTotal - remainingToDeduct);
                inventoryService.adjustToTotal(dim, newTotal, WmsInventoryBizTypeEnum.STOCKTAKING_ADJUST, order.getOrderNo(),
                        "盘库单 " + order.getOrderNo() + " 盘亏出库调整(单号 " + ckCode + "): -" + remainingToDeduct);
            }

            // 损溢记录
            WmsStocktakingLoss loss = new WmsStocktakingLoss()
                    .setLossCount(shortage)
                    .setLossCategory(LOSS_CATEGORY_STOCKTAKING)
                    .setLossType(LOSS_TYPE_SHORTAGE)
                    .setStocktakingInfo(order.getId())
                    .setBatchNumber(order.getOrderNo())
                    .setCreateTime(now)
                    .setIsDel(1);
            lossMapper.insert(loss);
        }

        // 3. 回写盘库单
        order.setAdjustOutStatus(ADJUST_STATUS_DONE);
        order.setAdjustOutOrderNo(ckCode);
        if (order.getAdjustInStatus() != null && order.getAdjustInStatus() != ADJUST_STATUS_PENDING) {
            order.setStatus(STATUS_ADJUSTED);
        }
        this.updateById(order);

        log.info("盘库单 {} 成功生成盘亏出库单: {}, 盘亏数量: {}", order.getOrderNo(), ckCode, totalLossCount);
        return ckCode;
    }

    /**
     * 为某SKU定位目标库位
     */
    private WmsWarehouseAreaLoca findTargetLocaForSku(Integer warehouseId, Long skuId) {
        // 1. 优先查已有库存的库位
        List<WmsInventory> invList = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .eq(WmsInventory::getWarehouseId, warehouseId)
                .eq(WmsInventory::getSkuId, skuId)
                .eq(WmsInventory::getIsDel, 1)
                .gt(WmsInventory::getTotalCount, 0)
                .last("LIMIT 1"));
        if (!invList.isEmpty()) {
            return locaMapper.selectById(invList.get(0).getWarehouseAreaLocaId());
        }

        // 2. 兜底查仓库内第一个可用库位
        return locaMapper.selectOne(new LambdaQueryWrapper<WmsWarehouseAreaLoca>()
                .eq(WmsWarehouseAreaLoca::getWarehouseId, warehouseId)
                .eq(WmsWarehouseAreaLoca::getIsDel, 1)
                .last("LIMIT 1"));
    }

    /**
     * 生成盘库单号: PD + yyyyMMdd + 4位顺序/时间随机码
     */
    private synchronized String generateOrderNo() {
        String datePrefix = "PD" + new SimpleDateFormat("yyyyMMdd").format(new Date());
        long count = this.count(new LambdaQueryWrapper<WmsStocktakingOrder>()
                .likeRight(WmsStocktakingOrder::getOrderNo, datePrefix));
        return datePrefix + String.format("%04d", count + 1);
    }

    /**
     * 生成盘盈入库单号: RK + yyyyMMdd + 4位流水号
     */
    private synchronized String generateRkCode() {
        String datePrefix = "RK" + new SimpleDateFormat("yyyyMMdd").format(new Date());
        long count = warehousingEntryMapper.selectCount(new LambdaQueryWrapper<WmsWarehousingEntry>()
                .likeRight(WmsWarehousingEntry::getEntryCode, datePrefix));
        return datePrefix + String.format("%04d", count + 1);
    }

    /**
     * 生成盘亏出库单号: CK + yyyyMMdd + 4位流水号
     */
    private synchronized String generateCkCode() {
        String datePrefix = "CK" + new SimpleDateFormat("yyyyMMdd").format(new Date());
        long count = outboundOrderMapper.selectCount(new LambdaQueryWrapper<WmsOutboundOrder>()
                .likeRight(WmsOutboundOrder::getOutboundCode, datePrefix));
        return datePrefix + String.format("%04d", count + 1);
    }
}
