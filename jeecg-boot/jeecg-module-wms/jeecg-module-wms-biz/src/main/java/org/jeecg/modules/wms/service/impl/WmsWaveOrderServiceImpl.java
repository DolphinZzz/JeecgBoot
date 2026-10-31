package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.entity.WmsItemSku;
import org.jeecg.modules.wms.entity.WmsItemSpu;
import org.jeecg.modules.wms.entity.WmsOutboundOrder;
import org.jeecg.modules.wms.entity.WmsOutboundOrderDetails;
import org.jeecg.modules.wms.entity.WmsWaveOrder;
import org.jeecg.modules.wms.enums.WmsOutboundStatusEnum;
import org.jeecg.modules.wms.enums.WmsPickModeEnum;
import org.jeecg.modules.wms.enums.WmsWaveStatusEnum;
import org.jeecg.modules.wms.mapper.WmsItemSkuMapper;
import org.jeecg.modules.wms.mapper.WmsItemSpuMapper;
import org.jeecg.modules.wms.mapper.WmsOutboundOrderDetailsMapper;
import org.jeecg.modules.wms.mapper.WmsOutboundOrderMapper;
import org.jeecg.modules.wms.mapper.WmsWaveOrderMapper;
import org.jeecg.modules.wms.service.IWmsWaveOrderService;
import org.jeecg.modules.wms.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 出库波次服务实现
 * @Author: jeecg-boot
 * @Date: 2026-10-31
 * @Version: V1.0
 */
@Slf4j
@Service
public class WmsWaveOrderServiceImpl extends ServiceImpl<WmsWaveOrderMapper, WmsWaveOrder>
        implements IWmsWaveOrderService {

    @Autowired
    private WmsOutboundOrderMapper outboundOrderMapper;

    @Autowired
    private WmsOutboundOrderDetailsMapper outboundOrderDetailsMapper;

    @Autowired
    private WmsItemSkuMapper itemSkuMapper;

    @Autowired
    private WmsItemSpuMapper itemSpuMapper;

    @Override
    public IPage<WmsWavePage> queryWavePage(WmsWaveQuery query, Integer pageNo, Integer pageSize) {
        Page<WmsWavePage> page = new Page<>(pageNo, pageSize);
        if (query == null) {
            query = new WmsWaveQuery();
        }
        return baseMapper.selectWavePage(page, query);
    }

    @Override
    public WmsWaveDetailView getWaveDetail(Long waveId) {
        WmsWaveOrder waveOrder = getById(waveId);
        if (waveOrder == null) {
            throw new JeecgBootException("波次不存在: id=" + waveId);
        }

        WmsWaveDetailView detailView = new WmsWaveDetailView();
        detailView.setWaveOrder(waveOrder);

        // 关联出库单清单
        WmsOutboundQuery q = new WmsOutboundQuery();
        q.setWaveId(waveId);
        Page<WmsOutboundPage> orderPage = new Page<>(1, 1000);
        IPage<WmsOutboundPage> orders = outboundOrderMapper.selectOutboundPage(orderPage, q);
        detailView.setOutboundOrders(orders.getRecords());

        // 汇总拣货任务行
        List<WmsWaveDetailView.WmsWavePickItem> pickItems = baseMapper.selectWavePickItems(waveId);
        detailView.setPickItems(pickItems);

        return detailView;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<WmsWaveOrder> generateWaves(WmsWaveGenerateForm form) {
        if (form.getWarehouseId() == null) {
            throw new JeecgBootException("仓库ID不能为空");
        }

        // 1. 查找满足分波条件的出库单：仓库匹配、已分配锁库(status=20)、拣货模式为波次播种法(pick_mode=2)、未分波(wave_id IS NULL)
        LambdaQueryWrapper<WmsOutboundOrder> qw = new LambdaQueryWrapper<WmsOutboundOrder>()
                .eq(WmsOutboundOrder::getWarehouseId, form.getWarehouseId())
                .eq(WmsOutboundOrder::getStatus, WmsOutboundStatusEnum.ALLOCATED.getCode())
                .eq(WmsOutboundOrder::getPickMode, WmsPickModeEnum.WAVE_SEED.getCode())
                .isNull(WmsOutboundOrder::getWaveId)
                .eq(WmsOutboundOrder::getIsDel, 1);

        if (form.getOutboundOrderIds() != null && !form.getOutboundOrderIds().isEmpty()) {
            qw.in(WmsOutboundOrder::getId, form.getOutboundOrderIds());
        }

        List<WmsOutboundOrder> candidateOrders = outboundOrderMapper.selectList(qw);
        if (candidateOrders.isEmpty()) {
            throw new JeecgBootException("未找到可分波的出库单（要求：状态为已分配、拣货模式为波次播种且未加入波次）");
        }

        // 2. 查出这些出库单的温区属性（通过明细 -> SKU -> SPU -> storage_condition）
        List<Long> orderIds = candidateOrders.stream().map(WmsOutboundOrder::getId).collect(Collectors.toList());
        List<WmsOutboundOrderDetails> allDetails = outboundOrderDetailsMapper.selectList(
                new LambdaQueryWrapper<WmsOutboundOrderDetails>()
                        .in(WmsOutboundOrderDetails::getOutboundOrderId, orderIds)
                        .eq(WmsOutboundOrderDetails::getIsDel, 1));

        Map<Long, String> orderStorageConditionMap = resolveOrderStorageConditions(allDetails);

        // 3. 决策 D3 核心：按「承运商 + 温区」分组
        // key: logisticsCompanyId + "_" + storageCondition
        Map<String, List<WmsOutboundOrder>> groupedOrders = new LinkedHashMap<>();
        for (WmsOutboundOrder order : candidateOrders) {
            Long logisticsId = order.getLogisticsCompanyId() != null ? order.getLogisticsCompanyId() : 0L;
            String cond = orderStorageConditionMap.getOrDefault(order.getId(), "常温");
            String groupKey = logisticsId + "_" + cond;
            groupedOrders.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(order);
        }

        // 4. 为每个组生成波次单
        List<WmsWaveOrder> generatedWaves = new ArrayList<>();
        String datePrefix = new SimpleDateFormat("yyyyMMdd").format(new Date());

        int seq = 1;
        for (Map.Entry<String, List<WmsOutboundOrder>> entry : groupedOrders.entrySet()) {
            List<WmsOutboundOrder> group = entry.getValue();
            if (group.isEmpty()) {
                continue;
            }

            WmsOutboundOrder first = group.get(0);
            Long logisticsId = first.getLogisticsCompanyId();
            String storageCondition = orderStorageConditionMap.getOrDefault(first.getId(), "常温");

            // 计算汇总统计
            int orderCount = group.size();
            int totalCount = group.stream().mapToInt(o -> o.getTotalCount() != null ? o.getTotalCount() : 0).sum();

            Set<Long> skuSet = new HashSet<>();
            for (WmsOutboundOrder o : group) {
                for (WmsOutboundOrderDetails d : allDetails) {
                    if (Objects.equals(d.getOutboundOrderId(), o.getId()) && d.getSkuId() != null) {
                        skuSet.add(d.getSkuId());
                    }
                }
            }

            String waveCode = generateWaveCode(datePrefix, seq++);

            WmsWaveOrder waveOrder = new WmsWaveOrder();
            waveOrder.setWaveCode(waveCode)
                    .setWarehouseId(form.getWarehouseId())
                    .setPickMode(WmsPickModeEnum.WAVE_SEED.getCode())
                    .setLogisticsCompanyId(logisticsId != null && logisticsId > 0 ? logisticsId : null)
                    .setStorageCondition(storageCondition)
                    .setOrderCount(orderCount)
                    .setSkuCount(skuSet.size())
                    .setTotalCount(totalCount)
                    .setStatus(WmsWaveStatusEnum.PENDING_PICK.getCode())
                    .setCreateTime(new Date());

            save(waveOrder);
            generatedWaves.add(waveOrder);

            // 回写该组出库单的 wave_id
            for (WmsOutboundOrder o : group) {
                o.setWaveId(waveOrder.getId());
                outboundOrderMapper.updateById(o);
            }
        }

        return generatedWaves;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startWavePick(Long waveId, Integer empId) {
        WmsWaveOrder waveOrder = getById(waveId);
        if (waveOrder == null) {
            throw new JeecgBootException("波次不存在: id=" + waveId);
        }
        if (waveOrder.getStatus() != WmsWaveStatusEnum.PENDING_PICK.getCode()) {
            throw new JeecgBootException("当前波次状态不是[待拣货]，无法开始拣货");
        }

        waveOrder.setStatus(WmsWaveStatusEnum.PICKING.getCode());
        waveOrder.setEmpId(empId);
        waveOrder.setUpdateTime(new Date());
        updateById(waveOrder);

        // 联动更新关联的所有出库单状态为 40 拣货中
        outboundOrderMapper.update(null,
                new LambdaUpdateWrapper<WmsOutboundOrder>()
                        .eq(WmsOutboundOrder::getWaveId, waveId)
                        .eq(WmsOutboundOrder::getIsDel, 1)
                        .set(WmsOutboundOrder::getStatus, WmsOutboundStatusEnum.PICKING.getCode())
                        .set(WmsOutboundOrder::getUpdateTime, new Date()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeWaveDistribute(Long waveId) {
        WmsWaveOrder waveOrder = getById(waveId);
        if (waveOrder == null) {
            throw new JeecgBootException("波次不存在: id=" + waveId);
        }
        if (waveOrder.getStatus() == WmsWaveStatusEnum.COMPLETED.getCode()) {
            throw new JeecgBootException("该波次已完成分货，请勿重复操作");
        }
        if (waveOrder.getStatus() == WmsWaveStatusEnum.CANCELLED.getCode()) {
            throw new JeecgBootException("该波次已取消，无法完成分货");
        }

        // 1. 波次状态变更为 5 已完成
        waveOrder.setStatus(WmsWaveStatusEnum.COMPLETED.getCode());
        waveOrder.setUpdateTime(new Date());
        updateById(waveOrder);

        // 2. 查出波次关联的所有出库单
        List<WmsOutboundOrder> orders = outboundOrderMapper.selectList(
                new LambdaQueryWrapper<WmsOutboundOrder>()
                        .eq(WmsOutboundOrder::getWaveId, waveId)
                        .eq(WmsOutboundOrder::getIsDel, 1));

        Date now = new Date();
        for (WmsOutboundOrder order : orders) {
            // 自动回填实拣数量并完成拣货
            List<WmsOutboundOrderDetails> details = outboundOrderDetailsMapper.selectList(
                    new LambdaQueryWrapper<WmsOutboundOrderDetails>()
                            .eq(WmsOutboundOrderDetails::getOutboundOrderId, order.getId())
                            .eq(WmsOutboundOrderDetails::getIsDel, 1));

            for (WmsOutboundOrderDetails d : details) {
                if (d.getPickedCount() == null || d.getPickedCount() == 0) {
                    d.setPickedCount(d.getAllocatedCount());
                }
                d.setPickStatus(2); // 已拣货
                d.setPickTime(now);
                outboundOrderDetailsMapper.updateById(d);
            }

            // 出库单状态推进至 50 拣货完成待复核
            order.setStatus(WmsOutboundStatusEnum.WAIT_REVIEW.getCode());
            order.setUpdateTime(now);
            outboundOrderMapper.updateById(order);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelWave(Long waveId) {
        WmsWaveOrder waveOrder = getById(waveId);
        if (waveOrder == null) {
            throw new JeecgBootException("波次不存在: id=" + waveId);
        }
        if (waveOrder.getStatus() == WmsWaveStatusEnum.COMPLETED.getCode()) {
            throw new JeecgBootException("波次已完成，禁止取消");
        }

        // 解除出库单关联，恢复出库单状态为 20 已分配
        List<WmsOutboundOrder> orders = outboundOrderMapper.selectList(
                new LambdaQueryWrapper<WmsOutboundOrder>()
                        .eq(WmsOutboundOrder::getWaveId, waveId)
                        .eq(WmsOutboundOrder::getIsDel, 1));

        Date now = new Date();
        for (WmsOutboundOrder order : orders) {
            order.setWaveId(null);
            if (order.getStatus() == WmsOutboundStatusEnum.PICKING.getCode()) {
                order.setStatus(WmsOutboundStatusEnum.ALLOCATED.getCode());
            }
            order.setUpdateTime(now);
            outboundOrderMapper.updateById(order);
        }

        waveOrder.setStatus(WmsWaveStatusEnum.CANCELLED.getCode());
        waveOrder.setUpdateTime(now);
        updateById(waveOrder);
    }

    /**
     * 解析每个出库单对应的存储温区
     */
    private Map<Long, String> resolveOrderStorageConditions(List<WmsOutboundOrderDetails> details) {
        Map<Long, String> res = new HashMap<>();
        if (details == null || details.isEmpty()) {
            return res;
        }

        Set<Long> skuIds = details.stream().map(WmsOutboundOrderDetails::getSkuId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (skuIds.isEmpty()) {
            return res;
        }

        List<WmsItemSku> skus = itemSkuMapper.selectBatchIds(skuIds);
        Map<Long, Long> skuToSpuMap = skus.stream().collect(Collectors.toMap(WmsItemSku::getId, WmsItemSku::getSpuId, (k1, k2) -> k1));

        Set<Long> spuIds = skus.stream().map(WmsItemSku::getSpuId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> spuConditionMap = new HashMap<>();
        if (!spuIds.isEmpty()) {
            List<WmsItemSpu> spus = itemSpuMapper.selectBatchIds(spuIds);
            for (WmsItemSpu s : spus) {
                if (s.getStorageCondition() != null && !s.getStorageCondition().isEmpty()) {
                    spuConditionMap.put(s.getId(), s.getStorageCondition());
                }
            }
        }

        for (WmsOutboundOrderDetails d : details) {
            Long spuId = skuToSpuMap.get(d.getSkuId());
            String cond = spuConditionMap.getOrDefault(spuId, "常温");
            res.putIfAbsent(d.getOutboundOrderId(), cond);
        }
        return res;
    }

    private String generateWaveCode(String datePrefix, int seq) {
        String base = "WV" + datePrefix;
        String full = base + String.format("%04d", seq);
        while (baseMapper.selectByCode(full) != null) {
            seq++;
            full = base + String.format("%04d", seq);
        }
        return full;
    }
}
