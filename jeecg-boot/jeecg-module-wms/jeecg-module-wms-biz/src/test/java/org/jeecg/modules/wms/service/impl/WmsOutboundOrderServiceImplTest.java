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
        form.setDetails(Collections.singletonList(new WmsOutboundDetailForm().setSkuId(10L).setWarehouseAreaLocaId(101).setOutCount(5)));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> outboundOrderService.createOutboundOrder(form));
        assertTrue(ex.getMessage().contains("MES工单领料出库必须录入MES生产工单号"));
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
        form.setDetails(Collections.singletonList(new WmsOutboundDetailForm().setSkuId(10L).setWarehouseAreaLocaId(101).setOutCount(5)));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> outboundOrderService.createOutboundOrder(form));
        assertTrue(ex.getMessage().contains("销售出库必须选择客户"));
    }

    @Test
    @DisplayName("调拨出库类型应被系统拦截拒绝")
    void testCreateOutbound_TransferType_ShouldThrow() {
        WmsOutboundCreateForm form = new WmsOutboundCreateForm();
        form.setWarehouseId(1);
        form.setOutboundType(4); // 调拨出库

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> outboundOrderService.createOutboundOrder(form));
        assertTrue(ex.getMessage().contains("不支持的出库类型"));
    }

    @Test
    @DisplayName("手工建单未指定出库库位应被拦截")
    void testCreateOutbound_WithoutLoca_ShouldThrow() {
        WmsWarehouse warehouse = new WmsWarehouse().setId(1).setName("标准仓").setWarehouseType("NORMAL");
        when(warehouseService.getById(1)).thenReturn(warehouse);

        WmsOutboundCreateForm form = new WmsOutboundCreateForm();
        form.setWarehouseId(1);
        form.setOutboundType(WmsOutboundTypeEnum.SALE.getCode());
        form.setCustomerId(10L);
        form.setDetails(Collections.singletonList(new WmsOutboundDetailForm().setSkuId(10L).setWarehouseAreaLocaId(null).setOutCount(5)));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> outboundOrderService.createOutboundOrder(form));
        assertTrue(ex.getMessage().contains("未指定出库库位"));
    }

    @Test
    @DisplayName("手工建单出库数超出库位可用库存时硬约束拦截")
    void testCreateOutbound_ExceedAvailableStock_ShouldThrow() {
        WmsWarehouse warehouse = new WmsWarehouse().setId(1).setName("标准仓").setWarehouseType("NORMAL");
        when(warehouseService.getById(1)).thenReturn(warehouse);

        WmsWarehouseAreaLoca loca = new WmsWarehouseAreaLoca();
        loca.setId(101);
        loca.setLocaCode("A-01-01");
        when(locaMapper.selectById(101)).thenReturn(loca);

        // 库位可用库存仅 10 件
        WmsInventory inv = new WmsInventory()
                .setId(1L)
                .setWarehouseId(1)
                .setWarehouseAreaId(1)
                .setWarehouseAreaLocaId(101)
                .setConsignorId(1L)
                .setSkuId(10L)
                .setBatchNumber("BATCH_01")
                .setQualityStatus(1)
                .setAvailableCount(10);
        when(inventoryMapper.selectList(any())).thenReturn(Collections.singletonList(inv));

        WmsOutboundCreateForm form = new WmsOutboundCreateForm();
        form.setWarehouseId(1);
        form.setOutboundType(WmsOutboundTypeEnum.SALE.getCode());
        form.setCustomerId(10L);
        form.setDetails(Collections.singletonList(
                new WmsOutboundDetailForm().setSkuId(10L).setWarehouseAreaLocaId(101).setOutCount(20) // 申请 20 件 > 10 件
        ));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> outboundOrderService.createOutboundOrder(form));
        assertTrue(ex.getMessage().contains("可用库存不足"));
    }

    @Test
    @DisplayName("手工建单指定合规库位成功：单头状态为20已分配且原子预占锁库")
    void testCreateOutbound_Success_AutoLockAndStatus20() {
        WmsWarehouse warehouse = new WmsWarehouse().setId(1).setName("标准仓").setWarehouseType("NORMAL");
        when(warehouseService.getById(1)).thenReturn(warehouse);

        WmsInventory inv = new WmsInventory()
                .setId(1L)
                .setWarehouseId(1)
                .setWarehouseAreaId(1)
                .setWarehouseAreaLocaId(101)
                .setConsignorId(2L)
                .setSkuId(10L)
                .setBatchNumber("BATCH_01")
                .setQualityStatus(1)
                .setAvailableCount(50);
        when(inventoryMapper.selectList(any())).thenReturn(Collections.singletonList(inv));

        WmsOutboundCreateForm form = new WmsOutboundCreateForm();
        form.setWarehouseId(1);
        form.setOutboundType(WmsOutboundTypeEnum.MES_WORK_ORDER.getCode());
        form.setMesWorkOrderNo("WO-20261102-01");
        form.setDetails(Collections.singletonList(
                new WmsOutboundDetailForm().setSkuId(10L).setWarehouseAreaLocaId(101).setOutCount(5)
        ));

        WmsOutboundOrder created = outboundOrderService.createOutboundOrder(form);
        assertNotNull(created);
        assertEquals(WmsOutboundStatusEnum.ALLOCATED.getCode(), created.getStatus()); // 状态 20
        assertEquals(2L, created.getConsignorId()); // 自动推导货主 2L
        assertEquals("WO-20261102-01", created.getMesWorkOrderNo());

        // 验证调用了原子预占锁库
        verify(inventoryService).applyChange(
                argThat(dim -> dim.getWarehouseAreaLocaId() == 101 && dim.getSkuId() == 10L && dim.getConsignorId() == 2L),
                eq(0), eq(5), eq(0),
                eq(WmsInventoryBizTypeEnum.LOCK), any(), any());
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
        verify(operationsDetailsMapper).insert(argThat((WmsWarehouseOperationsDetails d) -> d.getSourceCount() == 10 && d.getCategory() == 1 && d.getConsignorId() == 1L));
        // 单据状态为 91 已取消
        assertEquals(WmsOutboundStatusEnum.CANCELLED.getCode(), order.getStatus());
        assertEquals(0, detail.getAllocatedCount());
    }

    @Test
    @DisplayName("逆向取消：待发货状态无货主场景(OB202611020002)取消出库应安全生成返架作业并兜底货主与category")
    void testCancelOutbound_ReadyToShip_WithoutConsignor_GenerateReturnJob() {
        Long orderId = 102L;
        WmsOutboundOrder order = new WmsOutboundOrder()
                .setId(orderId)
                .setOutboundCode("OB202611020002")
                .setWarehouseId(1)
                .setConsignorId(null) // 无手工货主
                .setStatus(WmsOutboundStatusEnum.PACKED.getCode()) // 70 已打包待发货
                .setTotalCount(45)
                .setIsDel(1);
        when(outboundOrderMapper.selectById(orderId)).thenReturn(order);

        WmsOutboundOrderDetails detail = new WmsOutboundOrderDetails()
                .setId(202L)
                .setOutboundOrderId(orderId)
                .setSkuId(18L)
                .setTargetWarehouseAreaId(1)
                .setTargetWarehouseAreaLocaId(2)
                .setBatchNumber("BAT20261101B")
                .setConsignorId(null) // 明细无货主
                .setAllocatedCount(45)
                .setPickedCount(45)
                .setIsDel(1);
        when(outboundOrderDetailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail));

        outboundOrderService.cancelOutboundOrder(orderId, "取消整单重拣");

        // 验证锁库释放
        verify(inventoryService).applyChange(
                argThat(dim -> dim.getConsignorIdOrDefault() == 0L && dim.getWarehouseAreaLocaId() == 2),
                eq(0), eq(-45), eq(0), eq(WmsInventoryBizTypeEnum.UNLOCK), any(), any());

        // 验证生成取消返架作业单与明细完整性约束
        verify(operationsMapper).insert(argThat((WmsWarehouseOperations op) ->
                op.getOperationType() == 4
                        && op.getCategory() == 1
                        && op.getWarehouseId() == 1
                        && op.getJobCode().startsWith("RET")));

        verify(operationsDetailsMapper).insert(argThat((WmsWarehouseOperationsDetails d) ->
                d.getSourceCount() == 45
                        && d.getTargetCount() == 45
                        && d.getCategory() == 1
                        && d.getQualityStatus() == 1
                        && d.getConsignorId() == 0L));

        // 单据状态流转至 91
        assertEquals(WmsOutboundStatusEnum.CANCELLED.getCode(), order.getStatus());
        assertEquals(0, detail.getAllocatedCount());
    }

    @Test
    @DisplayName("出库复核：整单快速复核通过(无需明细行列表)推进至60待打包")
    void testConfirmReview_WithoutReviewLines_Pass_ShouldAdvanceTo60() {
        Long orderId = 100L;
        WmsOutboundOrder order = new WmsOutboundOrder()
                .setId(orderId)
                .setOutboundCode("OB202611020002")
                .setStatus(WmsOutboundStatusEnum.WAIT_REVIEW.getCode()) // 50 待复核
                .setIsDel(1);
        when(outboundOrderMapper.selectById(orderId)).thenReturn(order);

        WmsOutboundOrderDetails detail = new WmsOutboundOrderDetails()
                .setId(201L)
                .setOutboundOrderId(orderId)
                .setSkuId(10L)
                .setAllocatedCount(45)
                .setPickedCount(45)
                .setIsDel(1);
        when(outboundOrderDetailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail));

        WmsReviewConfirmForm form = new WmsReviewConfirmForm();
        form.setOutboundOrderId(orderId);
        form.setReviewStatus(1); // 1 复核通过
        form.setReviewRemark("商品完好，数量核对一致");
        form.setReviewLines(null); // 前端未传明细行列表

        outboundOrderService.confirmReview(form);

        // 验证明细状态与复核数量
        assertEquals(1, detail.getReviewStatus());
        assertEquals(45, detail.getReviewCount());
        verify(outboundOrderDetailsMapper).updateById(detail);

        // 验证单据状态推进至 60 复核通过待打包
        assertEquals(WmsOutboundStatusEnum.REVIEWED.getCode(), order.getStatus());
    }

    @Test
    @DisplayName("出库复核：整单复核异常时流转至92异常挂起")
    void testConfirmReview_WithoutReviewLines_Reject_ShouldAdvanceTo92() {
        Long orderId = 100L;
        WmsOutboundOrder order = new WmsOutboundOrder()
                .setId(orderId)
                .setOutboundCode("OB202611020002")
                .setStatus(WmsOutboundStatusEnum.WAIT_REVIEW.getCode())
                .setIsDel(1);
        when(outboundOrderMapper.selectById(orderId)).thenReturn(order);

        WmsOutboundOrderDetails detail = new WmsOutboundOrderDetails()
                .setId(201L)
                .setOutboundOrderId(orderId)
                .setSkuId(10L)
                .setAllocatedCount(45)
                .setPickedCount(45)
                .setIsDel(1);
        when(outboundOrderDetailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail));

        WmsReviewConfirmForm form = new WmsReviewConfirmForm();
        form.setOutboundOrderId(orderId);
        form.setReviewStatus(2); // 2 复核异常
        form.setReviewRemark("外包装破损破漏");

        outboundOrderService.confirmReview(form);

        // 验证明细状态为 2 不通过
        assertEquals(2, detail.getReviewStatus());
        assertEquals(0, detail.getReviewCount());

        // 验证单据状态推进至 92 异常挂起
        assertEquals(WmsOutboundStatusEnum.EXCEPTION_SUSPEND.getCode(), order.getStatus());
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

    @Test
    @DisplayName("打包称重：根据商品单重与实拣件数核算并推进至70待发货")
    void testPackageAndWeigh_WithWeightsList_ShouldUpdateStatusAndTotalWeight() {
        Long orderId = 100L;
        WmsOutboundOrder order = new WmsOutboundOrder()
                .setId(orderId)
                .setOutboundCode("OB202611020002")
                .setStatus(WmsOutboundStatusEnum.REVIEWED.getCode()) // 60 复核通过待打包
                .setIsDel(1);
        when(outboundOrderMapper.selectById(orderId)).thenReturn(order);

        WmsOutboundOrderDetails detail = new WmsOutboundOrderDetails()
                .setId(201L)
                .setOutboundOrderId(orderId)
                .setSkuId(18L)
                .setAllocatedCount(45)
                .setPickedCount(45)
                .setIsDel(1);
        when(outboundOrderDetailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail));

        WmsItemSku sku = new WmsItemSku().setId(18L).setSpuId(2L).setCode("SKU018").setProductWeight(340);
        WmsItemSpu spu = new WmsItemSpu().setId(2L).setTitle("纯棉卫衣").setWeightUnit("g");
        when(itemSkuMapper.selectById(18L)).thenReturn(sku);
        when(itemSpuMapper.selectById(2L)).thenReturn(spu);

        WmsPackageWeightForm form = new WmsPackageWeightForm();
        form.setOutboundOrderId(orderId);
        form.setActualWeightKg(15.600); // 15.3kg 净重 + 0.3kg 纸箱皮重

        WmsPackageWeightForm.WmsWeightBoxItem box = new WmsPackageWeightForm.WmsWeightBoxItem();
        box.setPackageCode("OB202611020002-BOX01");
        box.setPackageType("纸箱");
        box.setPackageCount(45);
        box.setActualWeight(15.600);
        box.setTheoreticalWeight(15.600);
        box.setDiffWeight(0.0);
        form.setWeights(Collections.singletonList(box));

        outboundOrderService.packageAndWeigh(form);

        // 验证单据状态推进至 70 待发货
        assertEquals(WmsOutboundStatusEnum.PACKED.getCode(), order.getStatus());
        assertEquals(15600, order.getTotalWeight()); // 15600 克
        verify(outboundOrderWeightsMapper).insert(any(WmsOutboundOrderWeights.class));
        verify(outboundOrderPackageMapper).insert(any(WmsOutboundOrderPackage.class));
    }

    @Test
    @DisplayName("明细列表：应正确转换并计算单件千克单重")
    void testListDetails_ShouldComputeUnitWeightAndKg() {
        Long orderId = 100L;
        WmsOutboundDetailView view = new WmsOutboundDetailView();
        view.setId(201L);
        view.setSkuId(18L);
        view.setPickedCount(45);
        view.setProductWeight(340);
        view.setWeightUnit("g");

        when(outboundOrderDetailsMapper.selectDetailViewsByOrderId(orderId)).thenReturn(Collections.singletonList(view));

        List<WmsOutboundDetailView> result = outboundOrderService.listDetails(orderId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(340, result.get(0).getUnitWeightG());
        assertEquals(0.34, result.get(0).getUnitWeightKg());
    }

    @Test
    @DisplayName("发货确认：应持久化物流运费、发货人与发运时间并推进至80已发货")
    void testConfirmShip_ShouldPersistFreightFeeAndShipTime() {
        Long orderId = 100L;
        WmsOutboundOrder order = new WmsOutboundOrder()
                .setId(orderId)
                .setOutboundCode("OB202611020003")
                .setStatus(WmsOutboundStatusEnum.PACKED.getCode()) // 70 已打包待发货
                .setWarehouseId(1)
                .setConsignorId(1L)
                .setIsDel(1);
        when(outboundOrderMapper.selectById(orderId)).thenReturn(order);

        WmsOutboundOrderDetails detail = new WmsOutboundOrderDetails()
                .setId(301L)
                .setOutboundOrderId(orderId)
                .setSkuId(18L)
                .setTargetWarehouseAreaId(1)
                .setTargetWarehouseAreaLocaId(2)
                .setAllocatedCount(45)
                .setPickedCount(45)
                .setBatchNumber("20261101")
                .setIsDel(1);
        when(outboundOrderDetailsMapper.selectList(any())).thenReturn(Collections.singletonList(detail));
        when(itemSnMapper.selectList(any())).thenReturn(Collections.emptyList());

        WmsShipConfirmForm form = new WmsShipConfirmForm();
        form.setOutboundOrderId(orderId);
        form.setLogisticsCompanyId(2L);
        form.setLogisticsNumber("SF1234567890");
        form.setFreightFee(35.50);

        outboundOrderService.confirmShip(form);

        // 验证单据状态推进至 80 已发货
        assertEquals(WmsOutboundStatusEnum.SHIPPED.getCode(), order.getStatus());
        assertEquals(2L, order.getLogisticsCompanyId());
        assertEquals("SF1234567890", order.getLogisticsNumber());
        assertEquals(35.50, order.getFreightFee());
        assertNotNull(order.getShipTime());
        verify(outboundOrderMapper).updateById(order);
    }

    @Test
    @DisplayName("箱单与称重及SN查询：验证数据正确关联并填充商品属性")
    void testListPackagesAndWeightsAndSns() {
        Long orderId = 100L;

        // 1. 测试包装箱单
        WmsOutboundOrderPackage pkg = new WmsOutboundOrderPackage()
                .setId(1L)
                .setOutboundOrderId(orderId)
                .setPackageCode("OB-BOX01")
                .setPackageType("纸箱")
                .setPackageCount(45)
                .setSkuId(18L);
        when(outboundOrderPackageMapper.selectList(any())).thenReturn(Collections.singletonList(pkg));
        WmsItemSku sku = new WmsItemSku().setId(18L).setCode("SKU018").setTitle("卫衣L码");
        when(itemSkuMapper.selectById(18L)).thenReturn(sku);

        List<WmsOutboundOrderPackage> packages = outboundOrderService.listPackages(orderId);
        assertEquals(1, packages.size());
        assertEquals("SKU018", packages.get(0).getSkuCode());
        assertEquals("卫衣L码", packages.get(0).getSkuName());

        // 2. 测试称重记录
        WmsOutboundOrderWeights weights = new WmsOutboundOrderWeights()
                .setId(1L)
                .setOutboundOrderId(orderId)
                .setPackageCode("OB-BOX01")
                .setActualWeight(15.6)
                .setTheoreticalWeight(15.6)
                .setDiffWeight(0.0);
        when(outboundOrderWeightsMapper.selectList(any())).thenReturn(Collections.singletonList(weights));

        List<WmsOutboundOrderWeights> weightList = outboundOrderService.listWeights(orderId);
        assertEquals(1, weightList.size());
        assertEquals(15.6, weightList.get(0).getActualWeight());

        // 3. 测试SN追溯明细
        WmsItemSn sn = new WmsItemSn()
                .setId(1L)
                .setOutboundOrderId(orderId)
                .setSnCode("SN20261102001")
                .setSkuId(18L)
                .setStatus(5);
        when(itemSnMapper.selectList(any())).thenReturn(Collections.singletonList(sn));

        List<WmsItemSn> snList = outboundOrderService.listSns(orderId);
        assertEquals(1, snList.size());
        assertEquals("SN20261102001", snList.get(0).getSnCode());
        assertEquals("SKU018", snList.get(0).getSkuCode());
        assertEquals("卫衣L码", snList.get(0).getSkuName());
    }
}
