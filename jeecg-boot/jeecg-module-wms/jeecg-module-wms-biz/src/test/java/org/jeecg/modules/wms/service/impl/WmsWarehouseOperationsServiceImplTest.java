package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.jeecg.common.exception.JeecgBootException;
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
import org.jeecg.modules.wms.vo.WmsAreaMigrationForm;
import org.jeecg.modules.wms.vo.WmsAreaMigrationPreviewVO;
import org.jeecg.modules.wms.vo.WmsInventoryDimension;
import org.jeecg.modules.wms.vo.WmsOperationDetailForm;
import org.jeecg.modules.wms.vo.WmsOperationForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 库内作业：移库执行时源库位扣减、目标库位增加，全仓总量不变。
 * 验收要点：执行前拦截已执行的作业单、无明细的作业单、目标库位不合规。
 */
@ExtendWith(MockitoExtension.class)
public class WmsWarehouseOperationsServiceImplTest {

    @Mock
    private WmsWarehouseOperationsMapper operationsMapper;

    @Mock
    private WmsWarehouseOperationsDetailsMapper detailsMapper;

    @Mock
    private WmsInventoryMapper inventoryMapper;

    @Mock
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Mock
    private WmsWarehouseAreaMapper areaMapper;

    @Mock
    private IWmsInventoryService inventoryService;

    @Mock
    private IWmsLocaCompatibilityService locaCompatibilityService;

    @Mock
    private WmsItemSkuMapper skuMapper;

    @Mock
    private WmsItemSpuMapper spuMapper;

    @InjectMocks
    private WmsWarehouseOperationsServiceImpl operationsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(operationsService, "baseMapper", operationsMapper);
        ReflectionTestUtils.setField(operationsService, "skuMapper", skuMapper);
        ReflectionTestUtils.setField(operationsService, "spuMapper", spuMapper);
    }

    private WmsWarehouseOperations pending(Long id) {
        return new WmsWarehouseOperations()
                .setId(id)
                .setJobCode("MV20261029120000001")
                .setWarehouseId(1)
                .setOperationType(1)
                .setStatus(WmsOperationStatusEnum.PENDING.getCode())
                .setIsDel(1);
    }

    private WmsWarehouseOperationsDetails detail(Long operationId) {
        return new WmsWarehouseOperationsDetails()
                .setId(100L)
                .setWarehouseOperationsId(operationId)
                .setSkuId(4L)
                .setBatchNumber("B001")
                .setConsignorId(0L)
                .setQualityStatus(1)
                .setSourceCount(10)
                .setTargetCount(10)
                .setSourceWarehouseAreaId(2)
                .setSourceWarehouseAreaLocaId(3)
                .setTargetWarehouseId(1)
                .setTargetWarehouseAreaId(5)
                .setTargetWarehouseAreaLocaId(6);
    }

    @Test
    void executeShouldDeductSourceAndIncreaseTarget() {
        when(operationsMapper.selectById(1L)).thenReturn(pending(1L));
        when(detailsMapper.selectList(ArgumentMatchers.<Wrapper<WmsWarehouseOperationsDetails>>any()))
                .thenReturn(Collections.singletonList(detail(1L)));
        when(locaMapper.selectById(3)).thenReturn(new WmsWarehouseAreaLoca().setId(3).setWarehouseId(1).setWarehouseAreaId(2));
        when(locaMapper.selectById(6)).thenReturn(new WmsWarehouseAreaLoca().setId(6).setWarehouseId(1).setWarehouseAreaId(5));
        when(inventoryService.getByDimension(any(WmsInventoryDimension.class))).thenReturn(null);

        Map<String, Object> result = operationsService.execute(1L);

        // 源库位扣 10，目标库位加 10，全仓总量不变
        verify(inventoryService).applyChange(
                ArgumentMatchers.<WmsInventoryDimension>argThat(d -> d.getWarehouseAreaLocaId() == 3),
                eq(-10), eq(0), eq(0), eq(WmsInventoryBizTypeEnum.MOVE_OUT), eq("MV20261029120000001"), anyString());
        verify(inventoryService).applyChange(
                ArgumentMatchers.<WmsInventoryDimension>argThat(d -> d.getWarehouseAreaLocaId() == 6),
                eq(10), eq(0), eq(0), eq(WmsInventoryBizTypeEnum.MOVE_IN), eq("MV20261029120000001"), anyString());
        verify(operationsMapper).updateById(ArgumentMatchers.<WmsWarehouseOperations>argThat(
                o -> o.getStatus() == WmsOperationStatusEnum.EXECUTED.getCode()));
        assertEquals(10L, result.get("movedCount"));
    }

    @Test
    void executeShouldRejectAlreadyExecutedOperation() {
        WmsWarehouseOperations executed = pending(2L).setStatus(WmsOperationStatusEnum.EXECUTED.getCode());
        when(operationsMapper.selectById(2L)).thenReturn(executed);

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> operationsService.execute(2L));
        assertTrue(ex.getMessage().contains("只有「未确认」的作业单可以执行"));
        verify(inventoryService, never()).applyChange(any(), anyInt(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    void executeShouldRejectOperationWithoutDetails() {
        when(operationsMapper.selectById(3L)).thenReturn(pending(3L));
        when(detailsMapper.selectList(ArgumentMatchers.<Wrapper<WmsWarehouseOperationsDetails>>any()))
                .thenReturn(Collections.emptyList());

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> operationsService.execute(3L));
        assertTrue(ex.getMessage().contains("没有明细"));
    }

    @Test
    void executeShouldRejectDetailWithoutTargetLoca() {
        WmsWarehouseOperationsDetails noTarget = detail(4L).setTargetWarehouseAreaLocaId(null);
        when(operationsMapper.selectById(4L)).thenReturn(pending(4L));
        when(detailsMapper.selectList(ArgumentMatchers.<Wrapper<WmsWarehouseOperationsDetails>>any()))
                .thenReturn(Collections.singletonList(noTarget));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> operationsService.execute(4L));
        assertTrue(ex.getMessage().contains("未指定移入库位"));
        verify(inventoryService, never()).applyChange(any(), anyInt(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    void createShouldRejectEmptyDetails() {
        WmsOperationForm form = new WmsOperationForm();
        form.setOperationType(1);
        form.setWarehouseId(1);
        form.setDetails(Collections.emptyList());

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> operationsService.createOperation(form));
        assertTrue(ex.getMessage().contains("请至少填写一行"));
    }

    @Test
    void createShouldRejectInvalidOperationType() {
        WmsOperationForm form = new WmsOperationForm();
        form.setOperationType(9);
        form.setWarehouseId(1);
        form.setDetails(Collections.singletonList(new WmsOperationDetailForm()));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> operationsService.createOperation(form));
        assertTrue(ex.getMessage().contains("作业子类只能是"));
    }

    @Test
    void createShouldRejectInsufficientSourceStock() {
        WmsOperationDetailForm line = new WmsOperationDetailForm();
        line.setSkuId(4L);
        line.setBatchNumber("B001");
        line.setSourceWarehouseAreaLocaId(3);
        line.setTargetWarehouseAreaLocaId(6);
        line.setCount(50);

        WmsOperationForm form = new WmsOperationForm();
        form.setOperationType(1);
        form.setWarehouseId(1);
        form.setDetails(Collections.singletonList(line));

        when(locaMapper.selectById(3)).thenReturn(new WmsWarehouseAreaLoca().setId(3).setWarehouseId(1).setWarehouseAreaId(2));
        when(inventoryService.getByDimension(any(WmsInventoryDimension.class)))
                .thenReturn(new WmsInventory().setId(10L).setAvailableCount(20));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> operationsService.createOperation(form));
        assertTrue(ex.getMessage().contains("可用库存不足"));
        verify(detailsMapper, never()).insert(ArgumentMatchers.<WmsWarehouseOperationsDetails>any());
    }

    @Test
    void cancelShouldRejectExecutedOperation() {
        when(operationsMapper.selectById(5L)).thenReturn(pending(5L).setStatus(WmsOperationStatusEnum.EXECUTED.getCode()));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> operationsService.cancel(5L));
        assertTrue(ex.getMessage().contains("已执行的作业单不能取消"));
    }

    @Test
    void cancelShouldMarkPendingOperationCancelled() {
        when(operationsMapper.selectById(6L)).thenReturn(pending(6L));

        operationsService.cancel(6L);

        verify(operationsMapper).updateById(ArgumentMatchers.<WmsWarehouseOperations>argThat(
                o -> o.getStatus() == WmsOperationStatusEnum.CANCELLED.getCode()));
    }

    @Test
    void executeShouldValidateTargetLocaCompatibility() {
        when(operationsMapper.selectById(7L)).thenReturn(pending(7L));
        when(detailsMapper.selectList(ArgumentMatchers.<Wrapper<WmsWarehouseOperationsDetails>>any()))
                .thenReturn(Collections.singletonList(detail(7L)));
        doThrow(new JeecgBootException("目标库位[01-A-03]处于盘点锁定状态"))
                .when(locaCompatibilityService).checkInbound(eq(6), eq(4L), eq("B001"), eq(10), any());

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> operationsService.execute(7L));
        assertTrue(ex.getMessage().contains("盘点锁定"));
        verify(inventoryService, never()).applyChange(any(), anyInt(), anyInt(), anyInt(), any(), any(), any());
        verify(operationsMapper, never()).updateById(ArgumentMatchers.<WmsWarehouseOperations>any());
    }

    @Test
    void generateReplenishShouldRejectWarehouseWithoutPickingArea() {
        when(areaMapper.selectList(ArgumentMatchers.<Wrapper<org.jeecg.modules.wms.entity.WmsWarehouseArea>>any()))
                .thenReturn(Collections.emptyList());

        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> operationsService.generateReplenish(1, List.of(4L)));
        assertTrue(ex.getMessage().contains("没有拣货区"));
    }

    @Test
    void previewAreaMigrationShouldRejectSameArea() {
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> operationsService.previewAreaMigration(1, 10, 1, 10));
        assertTrue(ex.getMessage().contains("源库区与目标库区不能相同"));
    }

    @Test
    void previewAreaMigrationShouldHandleEmptySourceArea() {
        when(areaMapper.selectById(10)).thenReturn(new WmsWarehouseArea().setId(10).setName("源库区A"));
        when(areaMapper.selectById(20)).thenReturn(new WmsWarehouseArea().setId(20).setName("目标库区B"));
        when(inventoryMapper.selectList(ArgumentMatchers.<Wrapper<WmsInventory>>any()))
                .thenReturn(Collections.emptyList());

        WmsAreaMigrationPreviewVO preview = operationsService.previewAreaMigration(1, 10, 1, 20);

        assertFalse(preview.getCanMigrate());
        assertEquals(0, preview.getSourceOccupiedLocaCount());
        assertTrue(preview.getMessage().contains("当前没有在库货物"));
    }

    @Test
    void previewAreaMigrationShouldAutoMatchWhenEmptyLocasSufficient() {
        when(areaMapper.selectById(10)).thenReturn(new WmsWarehouseArea().setId(10).setName("源库区A"));
        when(areaMapper.selectById(20)).thenReturn(new WmsWarehouseArea().setId(20).setName("目标库区B"));

        // 源库区有 2 个库位在库
        WmsInventory inv1 = new WmsInventory().setWarehouseAreaId(10).setWarehouseAreaLocaId(101).setSkuId(1L).setBatchNumber("B01").setTotalCount(5);
        WmsInventory inv2 = new WmsInventory().setWarehouseAreaId(10).setWarehouseAreaLocaId(102).setSkuId(2L).setBatchNumber("B02").setTotalCount(8);
        when(inventoryMapper.selectList(ArgumentMatchers.<Wrapper<WmsInventory>>argThat(w -> w != null)))
                .thenReturn(Arrays.asList(inv1, inv2)) // 第一次查源库区在库
                .thenReturn(Collections.emptyList());  // 第二次查目标库区在库(无占用)

        // 目标库区有 3 个空闲库位
        WmsWarehouseAreaLoca tgt1 = new WmsWarehouseAreaLoca().setId(201).setLocaCode("B-01");
        WmsWarehouseAreaLoca tgt2 = new WmsWarehouseAreaLoca().setId(202).setLocaCode("B-02");
        WmsWarehouseAreaLoca tgt3 = new WmsWarehouseAreaLoca().setId(203).setLocaCode("B-03");
        when(locaMapper.selectList(ArgumentMatchers.<Wrapper<WmsWarehouseAreaLoca>>any()))
                .thenReturn(Arrays.asList(tgt1, tgt2, tgt3));

        // 库位编码与商品主数据
        when(locaMapper.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(
                new WmsWarehouseAreaLoca().setId(101).setLocaCode("A-01"),
                new WmsWarehouseAreaLoca().setId(102).setLocaCode("A-02")
        ));
        when(skuMapper.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(
                new WmsItemSku().setId(1L).setCode("SKU001").setSpuId(1001L),
                new WmsItemSku().setId(2L).setCode("SKU002").setSpuId(1002L)
        ));
        when(spuMapper.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(
                new WmsItemSpu().setId(1001L).setTitle("商品A"),
                new WmsItemSpu().setId(1002L).setTitle("商品B")
        ));

        WmsAreaMigrationPreviewVO preview = operationsService.previewAreaMigration(1, 10, 1, 20);

        assertTrue(preview.getCanMigrate());
        assertEquals(2, preview.getSourceOccupiedLocaCount());
        assertEquals(13, preview.getSourceTotalItemCount());
        assertEquals(3, preview.getTargetEmptyLocaCount());
        assertEquals(0, preview.getMissingLocaCount());
        assertTrue(preview.getMissingSourceLocas().isEmpty());
        assertEquals(2, preview.getDetails().size());
        assertEquals("MATCHED", preview.getDetails().get(0).getMatchStatus());
        assertEquals("MATCHED", preview.getDetails().get(1).getMatchStatus());
        assertEquals(201, preview.getDetails().get(0).getTargetWarehouseAreaLocaId());
        assertEquals(202, preview.getDetails().get(1).getTargetWarehouseAreaLocaId());
        assertTrue(preview.getMessage().contains("预检通过"));
    }

    @Test
    void previewAreaMigrationShouldWarnWhenEmptyLocasInsufficient() {
        when(areaMapper.selectById(10)).thenReturn(new WmsWarehouseArea().setId(10).setName("源库区A"));
        when(areaMapper.selectById(20)).thenReturn(new WmsWarehouseArea().setId(20).setName("目标库区B"));

        // 源库区有 2 个库位在库
        WmsInventory inv1 = new WmsInventory().setWarehouseAreaId(10).setWarehouseAreaLocaId(101).setSkuId(1L).setBatchNumber("B01").setTotalCount(5);
        WmsInventory inv2 = new WmsInventory().setWarehouseAreaId(10).setWarehouseAreaLocaId(102).setSkuId(2L).setBatchNumber("B02").setTotalCount(8);
        when(inventoryMapper.selectList(ArgumentMatchers.<Wrapper<WmsInventory>>argThat(w -> w != null)))
                .thenReturn(Arrays.asList(inv1, inv2))
                .thenReturn(Collections.emptyList());

        // 目标库区仅有 1 个空闲库位 (不足，缺 1 个)
        WmsWarehouseAreaLoca tgt1 = new WmsWarehouseAreaLoca().setId(201).setLocaCode("B-01");
        when(locaMapper.selectList(ArgumentMatchers.<Wrapper<WmsWarehouseAreaLoca>>any()))
                .thenReturn(Collections.singletonList(tgt1));

        when(locaMapper.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(
                new WmsWarehouseAreaLoca().setId(101).setLocaCode("A-01"),
                new WmsWarehouseAreaLoca().setId(102).setLocaCode("A-02")
        ));
        when(skuMapper.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(
                new WmsItemSku().setId(1L).setCode("SKU001").setSpuId(1001L),
                new WmsItemSku().setId(2L).setCode("SKU002").setSpuId(1002L)
        ));
        when(spuMapper.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(
                new WmsItemSpu().setId(1001L).setTitle("商品A"),
                new WmsItemSpu().setId(1002L).setTitle("商品B")
        ));

        WmsAreaMigrationPreviewVO preview = operationsService.previewAreaMigration(1, 10, 1, 20);

        assertFalse(preview.getCanMigrate());
        assertEquals(2, preview.getSourceOccupiedLocaCount());
        assertEquals(1, preview.getTargetEmptyLocaCount());
        assertEquals(1, preview.getMissingLocaCount());
        assertEquals(List.of("A-02"), preview.getMissingSourceLocas());
        assertEquals(2, preview.getDetails().size());
        assertEquals("MATCHED", preview.getDetails().get(0).getMatchStatus());
        assertEquals("MISSING", preview.getDetails().get(1).getMatchStatus());
        assertNull(preview.getDetails().get(1).getTargetWarehouseAreaLocaId());
        assertTrue(preview.getMessage().contains("目标库区空闲库位不足"));
    }

    @Test
    void executeAreaMigrationShouldRejectDuplicateTargetLocas() {
        WmsAreaMigrationForm form = new WmsAreaMigrationForm();
        form.setSourceWarehouseId(1);
        form.setSourceWarehouseAreaId(10);
        form.setTargetWarehouseId(1);
        form.setTargetWarehouseAreaId(20);

        WmsOperationDetailForm d1 = new WmsOperationDetailForm();
        d1.setSourceWarehouseAreaLocaId(101);
        d1.setTargetWarehouseAreaLocaId(201);
        d1.setSkuId(1L);
        d1.setCount(5);

        WmsOperationDetailForm d2 = new WmsOperationDetailForm();
        d2.setSourceWarehouseAreaLocaId(102);
        d2.setTargetWarehouseAreaLocaId(201);
        d2.setSkuId(2L);
        d2.setCount(8);
        form.setDetails(Arrays.asList(d1, d2));

        when(locaMapper.selectById(201)).thenReturn(new WmsWarehouseAreaLoca().setId(201).setLocaCode("B-01"));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> operationsService.executeAreaMigration(form));
        assertTrue(ex.getMessage().contains("目标库位[B-01]被重复指定"));
    }

    @Test
    void createOperationWithCrossWarehouseShouldRecordTargetWarehouseId() {
        WmsOperationForm form = new WmsOperationForm();
        form.setOperationType(1);
        form.setWarehouseId(1);
        form.setTargetWarehouseId(5); // 跨仓：从 1 号仓到 5 号仓
        form.setRemark("跨仓调拨");

        WmsOperationDetailForm d = new WmsOperationDetailForm();
        d.setSkuId(10L);
        d.setCount(20);
        d.setSourceWarehouseAreaLocaId(101);
        d.setTargetWarehouseAreaLocaId(501);
        form.setDetails(List.of(d));

        // 移出库位在仓 1，移入库位在仓 5
        when(locaMapper.selectById(101)).thenReturn(new WmsWarehouseAreaLoca().setId(101).setWarehouseId(1).setLocaCode("01-A-01"));
        when(locaMapper.selectById(501)).thenReturn(new WmsWarehouseAreaLoca().setId(501).setWarehouseId(5).setLocaCode("GZ-01-A-01"));
        when(inventoryService.getByDimension(any())).thenReturn(new WmsInventory().setAvailableCount(50));

        WmsWarehouseOperations op = operationsService.createOperation(form);

        assertNotNull(op);
        assertEquals(1, op.getWarehouseId());
        assertEquals(5, op.getTargetWarehouseId());
        verify(operationsMapper).insert(ArgumentMatchers.<WmsWarehouseOperations>argThat(
                o -> o.getWarehouseId().equals(1) && o.getTargetWarehouseId().equals(5)));
        verify(detailsMapper).insert(ArgumentMatchers.<WmsWarehouseOperationsDetails>argThat(
                detail -> detail.getTargetWarehouseId().equals(5) && detail.getTargetWarehouseAreaLocaId().equals(501)));
    }

    @Test
    void createOperationCrossWarehouseShouldRejectLocaInWrongWarehouse() {
        WmsOperationForm form = new WmsOperationForm();
        form.setOperationType(1);
        form.setWarehouseId(1);
        form.setTargetWarehouseId(5);

        WmsOperationDetailForm d = new WmsOperationDetailForm();
        d.setSkuId(10L);
        d.setCount(20);
        d.setSourceWarehouseAreaLocaId(101);
        d.setTargetWarehouseAreaLocaId(501);
        form.setDetails(List.of(d));

        // 目标库位实际在仓 2，而非指定的目标仓 5
        when(locaMapper.selectById(101)).thenReturn(new WmsWarehouseAreaLoca().setId(101).setWarehouseId(1).setLocaCode("01-A-01"));
        when(locaMapper.selectById(501)).thenReturn(new WmsWarehouseAreaLoca().setId(501).setWarehouseId(2).setLocaCode("02-A-01"));
        when(inventoryService.getByDimension(any())).thenReturn(new WmsInventory().setAvailableCount(50));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> operationsService.createOperation(form));
        assertTrue(ex.getMessage().contains("不属于目标仓库"));
    }

    @Test
    void executeCrossWarehouseMoveShouldDeductSourceWhAndIncreaseTargetWh() {
        WmsWarehouseOperations op = pending(10L)
                .setWarehouseId(1)
                .setTargetWarehouseId(5); // 跨仓作业
        when(operationsMapper.selectById(10L)).thenReturn(op);

        WmsWarehouseOperationsDetails crossDetail = detail(10L)
                .setSourceWarehouseAreaLocaId(101)
                .setTargetWarehouseId(5)
                .setTargetWarehouseAreaLocaId(501)
                .setSourceCount(15);
        when(detailsMapper.selectList(any())).thenReturn(List.of(crossDetail));

        when(locaMapper.selectById(101)).thenReturn(new WmsWarehouseAreaLoca().setId(101).setWarehouseId(1).setWarehouseAreaId(10));
        when(locaMapper.selectById(501)).thenReturn(new WmsWarehouseAreaLoca().setId(501).setWarehouseId(5).setWarehouseAreaId(50));

        WmsInventory sourceInv = new WmsInventory().setProdDate(new java.util.Date()).setExpireDate(new java.util.Date());
        when(inventoryService.getByDimension(any())).thenReturn(sourceInv);

        Map<String, Object> res = operationsService.execute(10L);

        // 验证源仓库 1 扣减 15
        verify(inventoryService).applyChange(
                ArgumentMatchers.<WmsInventoryDimension>argThat(d -> d.getWarehouseId().equals(1) && d.getWarehouseAreaLocaId().equals(101)),
                eq(-15), eq(0), eq(0), eq(WmsInventoryBizTypeEnum.MOVE_OUT), anyString(), contains("跨仓调拨"));

        // 验证目标仓库 5 增加 15
        verify(inventoryService).applyChange(
                ArgumentMatchers.<WmsInventoryDimension>argThat(d -> d.getWarehouseId().equals(5) && d.getWarehouseAreaLocaId().equals(501)),
                eq(15), eq(0), eq(0), eq(WmsInventoryBizTypeEnum.MOVE_IN), anyString(), contains("跨仓调拨"));

        assertEquals(15L, res.get("movedCount"));
    }
}
