package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.entity.*;
import org.jeecg.modules.wms.enums.WmsInventoryBizTypeEnum;
import org.jeecg.modules.wms.enums.WmsOutboundStatusEnum;
import org.jeecg.modules.wms.enums.WmsOutboundTypeEnum;
import org.jeecg.modules.wms.enums.WmsShortageHandleEnum;
import org.jeecg.modules.wms.mapper.*;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.service.IWmsWarehouseService;
import org.jeecg.modules.wms.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 出库闭环全流程单元测试：
 * 涵盖建单校验、FEFO分配锁库、临期禁出拦截、SN校验锁定、复核称重、发货扣减、逆向取消返架
 */
@ExtendWith(MockitoExtension.class)
public class WmsOutboundOrderServiceImplTest {

    @Mock
    private WmsOutboundOrderMapper outboundOrderMapper;

    @Mock
    private WmsOutboundOrderDetailsMapper outboundOrderDetailsMapper;

    @Mock
    private WmsOutboundOrderPackageMapper outboundOrderPackageMapper;

    @Mock
    private WmsOutboundOrderWeightsMapper outboundOrderWeightsMapper;

    @Mock
    private IWmsInventoryService inventoryService;

    @Mock
    private WmsInventoryMapper inventoryMapper;

    @Mock
    private IWmsWarehouseService warehouseService;

    @Mock
    private WmsItemSkuMapper itemSkuMapper;

    @Mock
    private WmsItemSpuMapper itemSpuMapper;

    @Mock
    private WmsItemSnMapper itemSnMapper;

    @Mock
    private WmsWarehouseOperationsMapper operationsMapper;

    @Mock
    private WmsWarehouseOperationsDetailsMapper operationsDetailsMapper;

    @Mock
    private WmsWarehouseAreaLocaMapper locaMapper;

    @InjectMocks
    private WmsOutboundOrderServiceImpl outboundOrderService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(outboundOrderService, "baseMapper", outboundOrderMapper);
    }

    @Test
    @DisplayName("工业制造仓MES工单领料建单时若未录入工单号应被拦截")
    void testCreateOutbound_MesWithoutWorkOrder_ShouldThrow() {
        WmsWarehouse warehouse = new WmsWarehouse()
                .setId(1)
                .setName("总装工业仓")
                .setWarehouseType("INDUSTRIAL");
        when(warehouseService.getById(1)).thenReturn(warehouse);

        WmsOutboundCreateForm form = new WmsOutboundCreateForm();
        form.setWarehouseId(1);
        form.setOutboundType(WmsOutboundTypeEnum.MES_WORK_ORDER.getCode());
        form.setConsignorId(1L);
        form.setMesWorkOrderNo(""); // 未传工单号
        form.setDetails(Collections.singletonList(new WmsOutboundDetailForm().setSkuId(10L).setOutCount(5)));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> outboundOrderService.createOutboundOrder(form));
        assertTrue(ex.getMessage().contains("工业制造仓的 MES 工单领料出库必须绑定生产工单号"));
    }

    @Test
    @DisplayName("销售出库未选择客户应被拦截")
    void testCreateOutbound_SaleWithoutCustomer_ShouldThrow() {
        WmsWarehouse warehouse = new WmsWarehouse()
                .setId(1)
                .setName("中心仓")
                .setWarehouseType("NORMAL");
        when(warehouseService.getById(1)).thenReturn(warehouse);

        WmsOutboundCreateForm form = new WmsOutboundCreateForm();
        form.setWarehouseId(1);
        form.setOutboundType(WmsOutboundTypeEnum.SALE.getCode());
        form.setConsignorId(1L);
        form.setCustomerId(null); // 未选客户
        form.setDetails(Collections.singletonList(new WmsOutboundDetailForm().setSkuId(10L).setOutCount(5)));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> outboundOrderService.createOutboundOrder(form));
        assertTrue(ex.getMessage().contains("销售出库必须选择客户"));
    }

    @Test
    @DisplayName("库存分配：临期商品触发禁出拦截")
    void testAllocateInventory_NearExpire_ShouldSkip() {
        Long orderId = 100L;
        WmsOutboundOrder order = new WmsOutboundOrder()
                .setId(orderId)
                .setOutboundCode("OB202610310001")
                .setWarehouseId(1)
                .setConsignorId(1L)
                .setStatus(WmsOutboundStatusEnum.PENDING_ALLOCATE.getCode())
                .setIsDel(1);
        when(outboundOrderMapper.selectById(orderId)).thenReturn(order);

        WmsOutboundOrderDetails detail = new WmsOutboundOrderDetails()
                .setId(201L)
                .setOutboundOrderId(orderId)
                .setSkuId(10L)
                .setOutCount(10)
                .setAllocatedCount(0)
                .setShortageHandle(WmsShortageHandleEnum.REMOVE_SHORTAGE.getCode())
                .setIsDel(1);
        when(outboundOrderDetailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail));

        // SKU 与 SPU 临期禁出天数为 30 天
        WmsItemSku sku = new WmsItemSku().setId(10L).setSpuId(5L).setCode("SKU001");
        WmsItemSpu spu = new WmsItemSpu().setId(5L).setTitle("生鲜酸奶").setNearExpireDays(30);
        when(itemSkuMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(sku));
        when(itemSpuMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(spu));

        // 库存中的批次仅剩 10 天过期（<= 30天禁出阈值）
        Date nearExpireDate = Date.from(LocalDate.now().plusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant());
        WmsInventory nearInv = new WmsInventory()
                .setId(1L)
                .setWarehouseId(1)
                .setWarehouseAreaId(1)
                .setWarehouseAreaLocaId(101)
                .setConsignorId(1L)
                .setSkuId(10L)
                .setBatchNumber("BATCH_NEAR")
                .setQualityStatus(1)
                .setAvailableCount(50)
                .setExpireDate(nearExpireDate);
        when(inventoryMapper.selectList(any())).thenReturn(Collections.singletonList(nearInv));

        WmsOutboundAllocateResultVo result = outboundOrderService.allocateInventory(orderId);

        // 临期批次被跳过，导致缺货
        assertNotNull(result);
        assertEquals(1, result.getLineResults().size());
        assertTrue(result.getLineResults().get(0).getIsShortage());
        assertEquals(10, result.getLineResults().get(0).getShortageCount());
        // 未调用锁库
        verify(inventoryService, never()).applyChange(any(), anyInt(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("库存分配：缺货处理策略为整单挂起时应回滚锁库并置单据状态为30待补货")
    void testAllocateInventory_ShortageSuspend_RollbackLock() {
        Long orderId = 100L;
        WmsOutboundOrder order = new WmsOutboundOrder()
                .setId(orderId)
                .setOutboundCode("OB202610310002")
                .setWarehouseId(1)
                .setConsignorId(1L)
                .setStatus(WmsOutboundStatusEnum.PENDING_ALLOCATE.getCode())
                .setIsDel(1);
        when(outboundOrderMapper.selectById(orderId)).thenReturn(order);

        WmsOutboundOrderDetails detail = new WmsOutboundOrderDetails()
                .setId(201L)
                .setOutboundOrderId(orderId)
                .setSkuId(10L)
                .setOutCount(20)
                .setAllocatedCount(0)
                .setShortageHandle(WmsShortageHandleEnum.SUSPEND_ORDER.getCode()) // 缺货整单挂起
                .setIsDel(1);
        when(outboundOrderDetailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail));

        WmsItemSku sku = new WmsItemSku().setId(10L).setSpuId(5L).setCode("SKU001");
        when(itemSkuMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(sku));

        // 仅有 10 件可用库存（不足 20 件）
        WmsInventory inv = new WmsInventory()
                .setId(1L)
                .setWarehouseId(1)
                .setWarehouseAreaId(1)
                .setWarehouseAreaLocaId(101)
                .setConsignorId(1L)
                .setSkuId(10L)
                .setBatchNumber("BATCH_001")
                .setQualityStatus(1)
                .setAvailableCount(10)
                .setExpireDate(Date.from(LocalDate.now().plusDays(100).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        when(inventoryMapper.selectList(any())).thenReturn(Collections.singletonList(inv));

        WmsOutboundAllocateResultVo result = outboundOrderService.allocateInventory(orderId);

        assertNotNull(result);
        assertEquals(WmsOutboundStatusEnum.WAIT_REPLENISH.getCode(), result.getStatus());
        // 验证先 LOCK 后 UNLOCK
        verify(inventoryService, times(1)).applyChange(any(), eq(0), eq(10), eq(0), eq(WmsInventoryBizTypeEnum.LOCK), any(), any());
        verify(inventoryService, times(1)).applyChange(any(), eq(0), eq(-10), eq(0), eq(WmsInventoryBizTypeEnum.UNLOCK), any(), any());
    }

    @Test
    @DisplayName("实拣确认：启用SN管理商品必须录入良品SN并流转为拣货锁定")
    void testConfirmPick_WithSnValidation() {
        Long orderId = 100L;
        WmsOutboundOrder order = new WmsOutboundOrder()
                .setId(orderId)
                .setOutboundCode("OB202610310003")
                .setStatus(WmsOutboundStatusEnum.ALLOCATED.getCode())
                .setIsDel(1);
        when(outboundOrderMapper.selectById(orderId)).thenReturn(order);

        WmsOutboundOrderDetails detail = new WmsOutboundOrderDetails()
                .setId(201L)
                .setOutboundOrderId(orderId)
                .setSkuId(10L)
                .setAllocatedCount(2)
                .setIsDel(1);
        when(outboundOrderDetailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail));

        WmsItemSku sku = new WmsItemSku().setId(10L).setIsSnManaged(1).setTitle("高精密传感器");
        when(itemSkuMapper.selectById(10L)).thenReturn(sku);

        WmsItemSn sn1 = new WmsItemSn().setId(1L).setSnCode("SN001").setSkuId(10L).setStatus(2).setIsDel(1);
        WmsItemSn sn2 = new WmsItemSn().setId(2L).setSnCode("SN002").setSkuId(10L).setStatus(2).setIsDel(1);
        when(itemSnMapper.selectOne(argThat(qw -> qw != null))).thenReturn(sn1, sn2);

        WmsPickConfirmForm form = new WmsPickConfirmForm();
        form.setOutboundOrderId(orderId);
        WmsPickConfirmForm.WmsPickLineItem line = new WmsPickConfirmForm.WmsPickLineItem();
        line.setDetailId(201L);
        line.setPickedCount(2);
        line.setSnCodes(Arrays.asList("SN001", "SN002"));
        form.setPickLines(Collections.singletonList(line));

        outboundOrderService.confirmPick(form);

        // 验证 SN 状态变更为 4 拣货锁定
        assertEquals(4, sn1.getStatus());
        assertEquals(orderId, sn1.getOutboundOrderId());
        verify(itemSnMapper, times(2)).updateById(any(WmsItemSn.class));
        // 验证单头状态推进至 50 待复核
        assertEquals(WmsOutboundStatusEnum.WAIT_REVIEW.getCode(), order.getStatus());
    }

    @Test
    @DisplayName("发货确认：扣减物理库存、SN标记为已出库、订单状态流转至80已发货")
    void testConfirmShip_DeductInventoryAndComplete() {
        Long orderId = 100L;
        WmsOutboundOrder order = new WmsOutboundOrder()
                .setId(orderId)
                .setOutboundCode("OB202610310004")
                .setWarehouseId(1)
                .setConsignorId(1L)
                .setStatus(WmsOutboundStatusEnum.PACKED.getCode())
                .setIsDel(1);
        when(outboundOrderMapper.selectById(orderId)).thenReturn(order);

        WmsOutboundOrderDetails detail = new WmsOutboundOrderDetails()
                .setId(201L)
                .setOutboundOrderId(orderId)
                .setSkuId(10L)
                .setTargetWarehouseAreaId(2)
                .setTargetWarehouseAreaLocaId(201)
                .setBatchNumber("BATCH_A")
                .setPickedCount(5)
                .setIsDel(1);
        when(outboundOrderDetailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail));

        WmsItemSn sn = new WmsItemSn().setId(1L).setSnCode("SN_X").setStatus(4).setIsDel(1);
        when(itemSnMapper.selectList(any())).thenReturn(Collections.singletonList(sn));

        WmsShipConfirmForm form = new WmsShipConfirmForm();
        form.setOutboundOrderId(orderId);
        form.setLogisticsCompanyId(1L);
        form.setLogisticsNumber("SF1234567890");

        outboundOrderService.confirmShip(form);

        // 验证物理库存与锁定库存扣减
        verify(inventoryService).applyChange(
                argThat(dim -> dim.getWarehouseAreaLocaId() == 201 && dim.getSkuId() == 10L),
                eq(-5), eq(-5), eq(0),
                eq(WmsInventoryBizTypeEnum.OUTBOUND_DEDUCT), eq(order.getOutboundCode()), any());

        // 验证 SN 变更为 5 已出库
        assertEquals(5, sn.getStatus());
        // 验证单据状态为 80 已发货
        assertEquals(WmsOutboundStatusEnum.SHIPPED.getCode(), order.getStatus());
        assertEquals("SF1234567890", order.getLogisticsNumber());
    }

    @Test
    @DisplayName("逆向取消：已拣货取消应生成取消返架作业单并恢复SN与释放锁库")
    void testCancelOutbound_PickedOrder_GenerateReturnJob() {
        Long orderId = 100L;
        WmsOutboundOrder order = new WmsOutboundOrder()
                .setId(orderId)
                .setOutboundCode("OB202610310005")
                .setWarehouseId(1)
                .setConsignorId(1L)
                .setStatus(WmsOutboundStatusEnum.REVIEWED.getCode()) // 60 复核通过（已拣货）
                .setTotalCount(10)
                .setIsDel(1);
        when(outboundOrderMapper.selectById(orderId)).thenReturn(order);

        WmsOutboundOrderDetails detail = new WmsOutboundOrderDetails()
                .setId(201L)
                .setOutboundOrderId(orderId)
                .setSkuId(10L)
                .setTargetWarehouseAreaId(2)
                .setTargetWarehouseAreaLocaId(201)
                .setBatchNumber("BATCH_RET")
                .setAllocatedCount(10)
                .setPickedCount(10)
                .setIsDel(1);
        when(outboundOrderDetailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail));

        WmsItemSn sn = new WmsItemSn().setId(1L).setSnCode("SN_RET").setStatus(4).setOutboundOrderId(orderId).setIsDel(1);
        when(itemSnMapper.selectList(any())).thenReturn(Collections.singletonList(sn));

        outboundOrderService.cancelOutboundOrder(orderId, "客户临时退单");

        // 验证锁库释放
        verify(inventoryService).applyChange(any(), eq(0), eq(-10), eq(0), eq(WmsInventoryBizTypeEnum.UNLOCK), any(), any());
        // 验证 SN 恢复为 2 在库良品
        assertEquals(2, sn.getStatus());
        assertNull(sn.getOutboundOrderId());
        // 验证生成取消返架单（operation_type = 4）
        verify(operationsMapper).insert(argThat((WmsWarehouseOperations op) -> op.getOperationType() == 4));
        verify(operationsDetailsMapper).insert(argThat((WmsWarehouseOperationsDetails d) -> d.getSourceCount() == 10));
        // 单据状态为 91 已取消
        assertEquals(WmsOutboundStatusEnum.CANCELLED.getCode(), order.getStatus());
    }

    @Test
    @DisplayName("逆向取消：已发货订单禁止直接取消")
    void testCancelOutbound_ShippedOrder_ShouldThrow() {
        Long orderId = 100L;
        WmsOutboundOrder order = new WmsOutboundOrder()
                .setId(orderId)
                .setStatus(WmsOutboundStatusEnum.SHIPPED.getCode()); // 80 已发货
        when(outboundOrderMapper.selectById(orderId)).thenReturn(order);

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> outboundOrderService.cancelOutboundOrder(orderId, "退货"));
        assertTrue(ex.getMessage().contains("出库单已发运出库"));
    }
}
