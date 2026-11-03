package org.jeecg.modules.wms.service.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.entity.*;
import org.jeecg.modules.wms.enums.WmsInventoryBizTypeEnum;
import org.jeecg.modules.wms.mapper.*;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.service.IWmsStocktakingOrderService;
import org.jeecg.modules.wms.vo.WmsInventoryDimension;
import org.jeecg.modules.wms.vo.WmsStocktakingOrderForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WmsStocktakingOrderServiceImplTest {

    @Mock
    private WmsStocktakingOrderMapper stocktakingOrderMapper;

    @Mock
    private WmsStocktakingDetailMapper stocktakingDetailMapper;

    @Mock
    private WmsInventoryMapper inventoryMapper;

    @Mock
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Mock
    private IWmsInventoryService inventoryService;

    @Mock
    private WmsStocktakingLossMapper lossMapper;

    @Mock
    private WmsWarehousingEntryMapper warehousingEntryMapper;

    @Mock
    private WmsWarehousingEntryDetailsMapper warehousingEntryDetailsMapper;

    @Mock
    private WmsOutboundOrderMapper outboundOrderMapper;

    @Mock
    private WmsOutboundOrderDetailsMapper outboundOrderDetailsMapper;

    @InjectMocks
    private WmsStocktakingOrderServiceImpl stocktakingOrderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(stocktakingOrderService, "baseMapper", stocktakingOrderMapper);
    }

    @Test
    void testSaveDraft_success() {
        WmsStocktakingOrderForm form = new WmsStocktakingOrderForm();
        form.setWarehouseId(1);
        form.setOrderDate(new Date());
        form.setRemark("测试暂存盘库单");

        WmsStocktakingDetail d = new WmsStocktakingDetail();
        d.setSkuId(101L);
        d.setSkuCode("SKU101");
        d.setBookCount(50);
        d.setUnitPrice(new BigDecimal("100.00"));
        d.setActualCount(null); // 暂存时允许未盘
        form.setDetails(Collections.singletonList(d));

        when(stocktakingOrderMapper.selectCount(any())).thenReturn(0L);
        when(stocktakingOrderMapper.insert(any(WmsStocktakingOrder.class))).thenReturn(1);
        when(stocktakingDetailMapper.insert(any(WmsStocktakingDetail.class))).thenReturn(1);

        WmsStocktakingOrder result = stocktakingOrderService.saveDraft(form);

        assertNotNull(result);
        assertEquals(IWmsStocktakingOrderService.STATUS_DRAFT, result.getStatus());
        assertTrue(result.getOrderNo().startsWith("PD"));
        assertEquals(50, result.getTotalBookCount());
        verify(stocktakingOrderMapper).insert(any(WmsStocktakingOrder.class));
        verify(stocktakingDetailMapper).insert(any(WmsStocktakingDetail.class));
        verifyNoInteractions(inventoryService); // 暂存不修改库存
    }

    @Test
    void testComplete_locksData_doesNotAdjustInventory() {
        // 完成盘点：盘盈 5，盘亏 2。实盘锁定，状态流转为 1(已盘点)，绝不修改物理库存！
        WmsStocktakingOrderForm form = new WmsStocktakingOrderForm();
        form.setWarehouseId(1);
        form.setOrderDate(new Date());

        WmsStocktakingDetail d1 = new WmsStocktakingDetail();
        d1.setSkuId(101L);
        d1.setSkuCode("SKU101");
        d1.setBookCount(40);
        d1.setActualCount(45); // 盘盈 5
        d1.setUnitPrice(new BigDecimal("20.00"));

        WmsStocktakingDetail d2 = new WmsStocktakingDetail();
        d2.setSkuId(102L);
        d2.setSkuCode("SKU102");
        d2.setBookCount(50);
        d2.setActualCount(48); // 盘亏 2
        d2.setUnitPrice(new BigDecimal("50.00"));

        form.setDetails(List.of(d1, d2));

        when(stocktakingOrderMapper.selectCount(any())).thenReturn(0L);
        when(stocktakingOrderMapper.insert(any(WmsStocktakingOrder.class))).thenReturn(1);
        when(stocktakingDetailMapper.insert(any(WmsStocktakingDetail.class))).thenReturn(1);

        WmsStocktakingOrder result = stocktakingOrderService.complete(form);

        assertNotNull(result);
        // 关键断言 1：状态必须为 1(已盘点)，等待人工在台账中触发生成出入库单
        assertEquals(IWmsStocktakingOrderService.STATUS_COUNTED, result.getStatus());
        // 关键断言 2：盘盈盘亏指标正确统计
        assertEquals(5, result.getProfitCount());
        assertEquals(new BigDecimal("100.00"), result.getProfitAmount());
        assertEquals(2, result.getLossCount());
        assertEquals(new BigDecimal("100.00"), result.getLossAmount());
        assertEquals(IWmsStocktakingOrderService.ADJUST_STATUS_PENDING, result.getAdjustInStatus());
        assertEquals(IWmsStocktakingOrderService.ADJUST_STATUS_PENDING, result.getAdjustOutStatus());

        // 关键断言 3：绝不自动调物理库存！
        verifyNoInteractions(inventoryService);
    }

    @Test
    void testCreateProfitEntry_success() {
        Long orderId = 100L;
        WmsStocktakingOrder order = new WmsStocktakingOrder();
        order.setId(orderId);
        order.setOrderNo("PD202611030001");
        order.setWarehouseId(1);
        order.setStatus(IWmsStocktakingOrderService.STATUS_COUNTED);
        order.setAdjustInStatus(IWmsStocktakingOrderService.ADJUST_STATUS_PENDING);
        order.setAdjustOutStatus(IWmsStocktakingOrderService.ADJUST_STATUS_NONE); // 无盘亏

        when(stocktakingOrderMapper.selectById(orderId)).thenReturn(order);

        WmsStocktakingDetail d = new WmsStocktakingDetail();
        d.setSkuId(101L);
        d.setSkuCode("SKU101");
        d.setDiscrepancyCount(5); // 盘盈 5
        when(stocktakingDetailMapper.selectByOrderId(orderId)).thenReturn(Collections.singletonList(d));

        when(warehousingEntryMapper.selectCount(any())).thenReturn(0L);
        when(warehousingEntryMapper.insert(any(WmsWarehousingEntry.class))).thenReturn(1);
        when(warehousingEntryDetailsMapper.insert(any(WmsWarehousingEntryDetails.class))).thenReturn(1);

        WmsWarehouseAreaLoca loca = new WmsWarehouseAreaLoca();
        loca.setId(10);
        loca.setWarehouseAreaId(2);
        loca.setWarehouseId(1);
        loca.setLocaCode("LOCA-01");
        when(locaMapper.selectOne(any())).thenReturn(loca);

        WmsInventory current = new WmsInventory();
        current.setTotalCount(40);
        when(inventoryMapper.selectOne(any())).thenReturn(current);

        String rkCode = stocktakingOrderService.createProfitEntry(orderId);

        assertNotNull(rkCode);
        assertTrue(rkCode.startsWith("RK"));

        // 验证生成入库单
        verify(warehousingEntryMapper).insert(argThat((WmsWarehousingEntry entry) ->
                entry.getEntryType() == 4 && entry.getStatus() == 60 && entry.getTotalCount() == 5));

        // 验证库存调整增加 5
        verify(inventoryService).adjustToTotal(any(WmsInventoryDimension.class), eq(45),
                eq(WmsInventoryBizTypeEnum.STOCKTAKING_ADJUST), eq("PD202611030001"), contains("盘盈入库调整"));

        // 验证回写状态为已生成且单据状态变为已调账
        assertEquals(IWmsStocktakingOrderService.ADJUST_STATUS_DONE, order.getAdjustInStatus());
        assertEquals(rkCode, order.getAdjustInOrderNo());
        assertEquals(IWmsStocktakingOrderService.STATUS_ADJUSTED, order.getStatus());
    }

    @Test
    void testCreateLossOutbound_success() {
        Long orderId = 200L;
        WmsStocktakingOrder order = new WmsStocktakingOrder();
        order.setId(orderId);
        order.setOrderNo("PD202611030002");
        order.setWarehouseId(1);
        order.setStatus(IWmsStocktakingOrderService.STATUS_COUNTED);
        order.setAdjustInStatus(IWmsStocktakingOrderService.ADJUST_STATUS_NONE); // 无盘盈
        order.setAdjustOutStatus(IWmsStocktakingOrderService.ADJUST_STATUS_PENDING);

        when(stocktakingOrderMapper.selectById(orderId)).thenReturn(order);

        WmsStocktakingDetail d = new WmsStocktakingDetail();
        d.setSkuId(102L);
        d.setSkuCode("SKU102");
        d.setDiscrepancyCount(-3); // 盘亏 3
        when(stocktakingDetailMapper.selectByOrderId(orderId)).thenReturn(Collections.singletonList(d));

        when(outboundOrderMapper.selectCount(any())).thenReturn(0L);
        when(outboundOrderMapper.insert(any(WmsOutboundOrder.class))).thenReturn(1);
        when(outboundOrderDetailsMapper.insert(any(WmsOutboundOrderDetails.class))).thenReturn(1);

        WmsInventory invRow = new WmsInventory();
        invRow.setId(1L);
        invRow.setWarehouseId(1);
        invRow.setWarehouseAreaId(2);
        invRow.setWarehouseAreaLocaId(10);
        invRow.setSkuId(102L);
        invRow.setConsignorId(0L);
        invRow.setBatchNumber("");
        invRow.setQualityStatus(1);
        invRow.setTotalCount(50);
        when(inventoryMapper.selectList(any())).thenReturn(Collections.singletonList(invRow));

        WmsWarehouseAreaLoca loca = new WmsWarehouseAreaLoca();
        loca.setId(10);
        loca.setWarehouseAreaId(2);
        loca.setWarehouseId(1);
        loca.setLocaCode("LOCA-01");
        when(locaMapper.selectById(any())).thenReturn(loca);

        String ckCode = stocktakingOrderService.createLossOutbound(orderId);

        assertNotNull(ckCode);
        assertTrue(ckCode.startsWith("CK"));

        // 验证生成出库单主表及其非空必填字段
        verify(outboundOrderMapper).insert(argThat((WmsOutboundOrder outbound) ->
                outbound.getOutboundType() == 4 && outbound.getStatus() == 80 && outbound.getTotalCount() == 3
                        && outbound.getCustomerId() != null));

        // 验证生成出库单明细及其非空目标库位字段
        verify(outboundOrderDetailsMapper).insert(argThat((WmsOutboundOrderDetails detail) ->
                detail.getTargetWarehouseId() != null && detail.getTargetWarehouseAreaId() != null
                        && detail.getTargetWarehouseAreaLocaId() != null && detail.getOutCount() == 3));

        // 验证损溢台账及其非空单据ID
        verify(lossMapper).insert(argThat((WmsStocktakingLoss loss) ->
                loss.getStocktakingInfo() != null && loss.getLossCount() == 3));

        // 验证扣减库存 3
        verify(inventoryService).adjustToTotal(any(WmsInventoryDimension.class), eq(47),
                eq(WmsInventoryBizTypeEnum.STOCKTAKING_ADJUST), eq("PD202611030002"), contains("盘亏出库调整"));

        // 验证回写状态为已生成且单据状态变为已调账
        assertEquals(IWmsStocktakingOrderService.ADJUST_STATUS_DONE, order.getAdjustOutStatus());
        assertEquals(ckCode, order.getAdjustOutOrderNo());
        assertEquals(IWmsStocktakingOrderService.STATUS_ADJUSTED, order.getStatus());
    }

    @Test
    void testComplete_validation_missingActualCount() {
        WmsStocktakingOrderForm form = new WmsStocktakingOrderForm();
        form.setWarehouseId(1);
        form.setOrderDate(new Date());

        WmsStocktakingDetail d = new WmsStocktakingDetail();
        d.setSkuId(103L);
        d.setSkuCode("SKU103");
        d.setBookCount(10);
        d.setActualCount(null); // 未填
        form.setDetails(Collections.singletonList(d));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () ->
                stocktakingOrderService.complete(form));
        assertTrue(ex.getMessage().contains("实际库存必须填写"));
    }

    @Test
    void testDeleteOrder_adjustedFails() {
        WmsStocktakingOrder order = new WmsStocktakingOrder();
        order.setId(1L);
        order.setStatus(IWmsStocktakingOrderService.STATUS_ADJUSTED);
        when(stocktakingOrderMapper.selectById(1L)).thenReturn(order);

        JeecgBootException ex = assertThrows(JeecgBootException.class, () ->
                stocktakingOrderService.deleteOrder(1L));
        assertTrue(ex.getMessage().contains("已完成调账的盘库单不能删除"));
    }
}
