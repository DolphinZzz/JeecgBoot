package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.entity.WmsWarehouseOperations;
import org.jeecg.modules.wms.entity.WmsWarehouseOperationsDetails;
import org.jeecg.modules.wms.enums.WmsInventoryBizTypeEnum;
import org.jeecg.modules.wms.enums.WmsOperationStatusEnum;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseOperationsDetailsMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseOperationsMapper;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.service.IWmsLocaCompatibilityService;
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

    @InjectMocks
    private WmsWarehouseOperationsServiceImpl operationsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(operationsService, "baseMapper", operationsMapper);
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
                .when(locaCompatibilityService).checkTarget(eq(6), eq(4L), eq("B001"), eq(10));

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
}
