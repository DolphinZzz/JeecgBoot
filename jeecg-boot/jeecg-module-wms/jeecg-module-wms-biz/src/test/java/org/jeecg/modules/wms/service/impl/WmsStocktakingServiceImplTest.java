package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsStocktakingInfo;
import org.jeecg.modules.wms.entity.WmsStocktakingLoss;
import org.jeecg.modules.wms.enums.WmsInventoryBizTypeEnum;
import org.jeecg.modules.wms.enums.WmsStocktakingStatusEnum;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.mapper.WmsStocktakingInfoMapper;
import org.jeecg.modules.wms.mapper.WmsStocktakingLossMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.service.IWmsInventoryService;
import org.jeecg.modules.wms.vo.WmsInventoryDimension;
import org.jeecg.modules.wms.vo.WmsStocktakingCountForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 盘点：实盘数录入 → 差异审批 → 结案调账的状态机与调账口径。
 * 验收要点 6.x：只有审批通过的差异才调整库存；无差异不写流水；已结案不可再改。
 */
@ExtendWith(MockitoExtension.class)
public class WmsStocktakingServiceImplTest {

    /** 差异审批：无差异 */
    private static final int AUDIT_NONE = 0;
    /** 差异审批：待审批 */
    private static final int AUDIT_PENDING = 1;

    @Mock
    private WmsStocktakingInfoMapper stocktakingMapper;

    @Mock
    private WmsStocktakingLossMapper lossMapper;

    @Mock
    private WmsInventoryMapper inventoryMapper;

    @Mock
    private WmsWarehouseAreaMapper areaMapper;

    @Mock
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Mock
    private IWmsInventoryService inventoryService;

    @InjectMocks
    private WmsStocktakingServiceImpl stocktakingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(stocktakingService, "baseMapper", stocktakingMapper);
    }

    private WmsStocktakingInfo record(int status, Integer book, Integer correct, int auditStatus) {
        return new WmsStocktakingInfo()
                .setId(1L)
                .setBatchNo("ST20261029120000")
                .setTargetWarehouseId(1)
                .setTargetWarehouseAreaId(2)
                .setTargetWarehouseAreaLocaId(3)
                .setSkuId(4L)
                .setConsignorId(0L)
                .setBatchNumber("B001")
                .setQualityStatus(1)
                .setBookCount(book)
                .setCorrectCount(correct)
                .setDiscrepancyCount(correct == null ? null : correct - book)
                .setAuditStatus(auditStatus)
                .setStatus(status)
                .setIsDel(1);
    }

    @Test
    void submitCountShouldComputeDiscrepancyAndWaitForAudit() {
        when(stocktakingMapper.selectById(1L)).thenReturn(record(1, 100, null, AUDIT_NONE));

        WmsStocktakingCountForm form = new WmsStocktakingCountForm();
        form.setId(1L);
        form.setCorrectCount(96);
        stocktakingService.submitCount(form);

        verify(stocktakingMapper).updateById(ArgumentMatchers.<WmsStocktakingInfo>argThat(info ->
                info.getDiscrepancyCount() == -4
                        && info.getAuditStatus() == AUDIT_PENDING
                        && info.getStatus() == WmsStocktakingStatusEnum.COUNTED.getCode()));
    }

    @Test
    void submitCountShouldNotRequireAuditWhenNoDiscrepancy() {
        when(stocktakingMapper.selectById(1L)).thenReturn(record(1, 100, null, AUDIT_NONE));

        WmsStocktakingCountForm form = new WmsStocktakingCountForm();
        form.setId(1L);
        form.setCorrectCount(100);
        stocktakingService.submitCount(form);

        verify(stocktakingMapper).updateById(ArgumentMatchers.<WmsStocktakingInfo>argThat(info ->
                info.getDiscrepancyCount() == 0 && info.getAuditStatus() == AUDIT_NONE));
    }

    @Test
    void submitCountShouldRejectNegativeCount() {
        WmsStocktakingCountForm form = new WmsStocktakingCountForm();
        form.setId(1L);
        form.setCorrectCount(-1);

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> stocktakingService.submitCount(form));
        assertTrue(ex.getMessage().contains("实盘数量不能为负数"));
        verify(stocktakingMapper, never()).updateById(ArgumentMatchers.<WmsStocktakingInfo>any());
    }

    @Test
    void submitCountShouldRejectConfirmedRecord() {
        when(stocktakingMapper.selectById(1L))
                .thenReturn(record(WmsStocktakingStatusEnum.CONFIRMED.getCode(), 100, 100, AUDIT_NONE));

        WmsStocktakingCountForm form = new WmsStocktakingCountForm();
        form.setId(1L);
        form.setCorrectCount(90);

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> stocktakingService.submitCount(form));
        assertTrue(ex.getMessage().contains("已确认结案"));
    }

    @Test
    void auditShouldOnlyAcceptPendingDifference() {
        when(stocktakingMapper.selectById(1L)).thenReturn(record(2, 100, 96, AUDIT_NONE));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> stocktakingService.audit(1L, true, null));
        assertTrue(ex.getMessage().contains("只有「待审批」的差异记录可以审批"));
    }

    @Test
    void auditShouldMarkPassed() {
        when(stocktakingMapper.selectById(1L)).thenReturn(record(2, 100, 96, AUDIT_PENDING));

        stocktakingService.audit(1L, true, null);

        verify(stocktakingMapper).updateById(ArgumentMatchers.<WmsStocktakingInfo>argThat(
                info -> info.getAuditStatus() == 2 && info.getAuditTime() != null));
    }

    @Test
    void confirmShouldRejectUnapprovedDifference() {
        when(stocktakingMapper.selectById(1L)).thenReturn(record(2, 100, 96, AUDIT_PENDING));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> stocktakingService.confirm(1L));
        assertTrue(ex.getMessage().contains("请先完成差异审批"));
        verify(inventoryService, never()).adjustToTotal(any(), anyInt(), any(), any(), any());
    }

    @Test
    void confirmShouldRejectRejectedDifference() {
        when(stocktakingMapper.selectById(1L)).thenReturn(record(2, 100, 96, 3));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> stocktakingService.confirm(1L));
        assertTrue(ex.getMessage().contains("差异已被驳回"));
        verify(inventoryService, never()).adjustToTotal(any(), anyInt(), any(), any(), any());
    }

    @Test
    void confirmShouldRejectBeforeCounting() {
        when(stocktakingMapper.selectById(1L)).thenReturn(record(1, 100, null, AUDIT_NONE));

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> stocktakingService.confirm(1L));
        assertTrue(ex.getMessage().contains("请先录入实盘数量"));
    }

    @Test
    void confirmShouldAdjustStockAndWriteShortageLoss() {
        when(stocktakingMapper.selectById(1L)).thenReturn(record(2, 100, 96, 2));
        when(stocktakingMapper.selectCount(ArgumentMatchers.<Wrapper<WmsStocktakingInfo>>any())).thenReturn(0L);
        when(stocktakingMapper.selectList(ArgumentMatchers.<Wrapper<WmsStocktakingInfo>>any()))
                .thenReturn(Collections.singletonList(record(3, 100, 96, 2)));

        Map<String, Object> result = stocktakingService.confirm(1L);

        // 盘亏 4：库存调到实盘数 96，写一条盘亏损溢记录
        verify(inventoryService).adjustToTotal(
                ArgumentMatchers.<WmsInventoryDimension>argThat(d -> d.getWarehouseAreaLocaId() == 3 && d.getSkuId() == 4L),
                eq(96), eq(WmsInventoryBizTypeEnum.STOCKTAKING_ADJUST), eq("ST20261029120000"), anyString());
        verify(lossMapper).insert(ArgumentMatchers.<WmsStocktakingLoss>argThat(loss ->
                loss.getLossType() == 1 && loss.getLossCount() == 4 && loss.getStocktakingInfo().equals(1L)));
        assertEquals(4, result.get("shortageCount"));
        assertEquals(0, result.get("surplusCount"));
    }

    @Test
    void confirmShouldWriteSurplusLossWhenOverCounted() {
        when(stocktakingMapper.selectById(1L)).thenReturn(record(2, 100, 105, 2));
        when(stocktakingMapper.selectCount(ArgumentMatchers.<Wrapper<WmsStocktakingInfo>>any())).thenReturn(0L);
        when(stocktakingMapper.selectList(ArgumentMatchers.<Wrapper<WmsStocktakingInfo>>any()))
                .thenReturn(Collections.singletonList(record(3, 100, 105, 2)));

        Map<String, Object> result = stocktakingService.confirm(1L);

        verify(lossMapper).insert(ArgumentMatchers.<WmsStocktakingLoss>argThat(
                loss -> loss.getLossType() == 2 && loss.getLossCount() == 5));
        assertEquals(5, result.get("surplusCount"));
    }

    @Test
    void confirmShouldNotTouchStockWhenNoDiscrepancy() {
        when(stocktakingMapper.selectById(1L)).thenReturn(record(2, 100, 100, AUDIT_NONE));
        when(stocktakingMapper.selectCount(ArgumentMatchers.<Wrapper<WmsStocktakingInfo>>any())).thenReturn(0L);
        when(stocktakingMapper.selectList(ArgumentMatchers.<Wrapper<WmsStocktakingInfo>>any()))
                .thenReturn(Collections.singletonList(record(3, 100, 100, AUDIT_NONE)));

        Map<String, Object> result = stocktakingService.confirm(1L);

        verify(inventoryService, never()).adjustToTotal(any(), anyInt(), any(), any(), any());
        verify(lossMapper, never()).insert(ArgumentMatchers.<WmsStocktakingLoss>any());
        verify(stocktakingMapper).updateById(ArgumentMatchers.<WmsStocktakingInfo>argThat(
                info -> info.getStatus() == WmsStocktakingStatusEnum.CONFIRMED.getCode()));
        assertTrue(String.valueOf(result.get("message")).contains("无差异"));
    }
}
