package org.jeecg.modules.wms.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.entity.WmsInventory;
import org.jeecg.modules.wms.entity.WmsInventoryLog;
import org.jeecg.modules.wms.enums.WmsInventoryBizTypeEnum;
import org.jeecg.modules.wms.mapper.WmsInventoryLogMapper;
import org.jeecg.modules.wms.mapper.WmsInventoryMapper;
import org.jeecg.modules.wms.vo.WmsInventoryDimension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 库存底座：数量恒等式与流水写入。
 * 验收要点 7.1：available + locked + frozen = total 恒成立，流水与库存一一对应。
 */
@ExtendWith(MockitoExtension.class)
public class WmsInventoryServiceImplTest {

    @Mock
    private WmsInventoryMapper inventoryMapper;

    @Mock
    private WmsInventoryLogMapper inventoryLogMapper;

    @InjectMocks
    private WmsInventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inventoryService, "baseMapper", inventoryMapper);
        ReflectionTestUtils.setField(inventoryService, "inventoryLogMapper", inventoryLogMapper);
    }

    private WmsInventoryDimension dim() {
        return new WmsInventoryDimension()
                .setWarehouseId(1)
                .setWarehouseAreaId(2)
                .setWarehouseAreaLocaId(3)
                .setSkuId(4L)
                .setBatchNumber("B001")
                .setQualityStatus(1);
    }

    private WmsInventory row(int total, int available, int locked, int frozen) {
        return new WmsInventory()
                .setId(10L)
                .setWarehouseId(1)
                .setWarehouseAreaId(2)
                .setWarehouseAreaLocaId(3)
                .setConsignorId(0L)
                .setSkuId(4L)
                .setBatchNumber("B001")
                .setQualityStatus(1)
                .setTotalCount(total)
                .setAvailableCount(available)
                .setLockedCount(locked)
                .setFrozenCount(frozen)
                .setIsDel(1);
    }

    @Test
    void putawayShouldIncreaseTotalAndAvailable() {
        when(inventoryMapper.selectByDimensionForUpdate(any())).thenReturn(row(100, 100, 0, 0));
        when(inventoryMapper.updateCounts(anyLong(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(1);

        WmsInventory result = inventoryService.applyChange(dim(), 50, 0, 0,
                WmsInventoryBizTypeEnum.INBOUND_PUTAWAY, "IN001", "上架");

        assertEquals(150, result.getTotalCount());
        assertEquals(150, result.getAvailableCount());
        verify(inventoryMapper).updateCounts(10L, 150, 150, 0, 0, 1);
        verify(inventoryLogMapper).insert(ArgumentMatchers.<WmsInventoryLog>argThat(log -> log.getChangeCount() == 50 && log.getAfterCount() == 150));
    }

    @Test
    void lockShouldKeepTotalAndMoveAvailableIntoLocked() {
        when(inventoryMapper.selectByDimensionForUpdate(any())).thenReturn(row(100, 100, 0, 0));
        when(inventoryMapper.updateCounts(anyLong(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(1);

        WmsInventory result = inventoryService.applyChange(dim(), 0, 30, 0,
                WmsInventoryBizTypeEnum.LOCK, "OUT001", "出库锁库");

        assertEquals(100, result.getTotalCount());
        assertEquals(30, result.getLockedCount());
        assertEquals(70, result.getAvailableCount());
        assertEquals(result.getTotalCount(), result.getAvailableCount() + result.getLockedCount() + result.getFrozenCount());
    }

    @Test
    void shouldRejectWhenAvailableWouldGoNegative() {
        when(inventoryMapper.selectByDimensionForUpdate(any())).thenReturn(row(100, 100, 0, 0));

        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> inventoryService.applyChange(dim(), -120, 0, 0,
                        WmsInventoryBizTypeEnum.OUTBOUND_DEDUCT, "OUT002", "出库"));
        assertTrue(ex.getMessage().contains("库存不足"));
        verify(inventoryMapper, never()).updateCounts(anyLong(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(inventoryLogMapper, never()).insert(ArgumentMatchers.<WmsInventoryLog>any());
    }

    @Test
    void shouldRejectWhenLockedExceedsTotal() {
        when(inventoryMapper.selectByDimensionForUpdate(any())).thenReturn(row(10, 10, 0, 0));

        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> inventoryService.applyChange(dim(), 0, 20, 0,
                        WmsInventoryBizTypeEnum.LOCK, "OUT003", "超量锁库"));
        assertTrue(ex.getMessage().contains("可用库存不足"));
    }

    @Test
    void adjustToTotalShouldWriteStocktakingLog() {
        when(inventoryMapper.selectByDimensionForUpdate(any())).thenReturn(row(100, 100, 0, 0));
        when(inventoryMapper.updateCounts(anyLong(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(1);

        WmsInventory result = inventoryService.adjustToTotal(dim(), 96,
                WmsInventoryBizTypeEnum.STOCKTAKING_ADJUST, "ST001", "盘亏");

        assertEquals(96, result.getTotalCount());
        assertEquals(96, result.getAvailableCount());
        verify(inventoryLogMapper).insert(ArgumentMatchers.<WmsInventoryLog>argThat(log ->
                log.getBizType() == WmsInventoryBizTypeEnum.STOCKTAKING_ADJUST.getCode() && log.getChangeCount() == -4));
    }

    @Test
    void adjustToSameTotalShouldNotTouchDatabase() {
        when(inventoryMapper.selectByDimensionForUpdate(any())).thenReturn(row(100, 100, 0, 0));

        WmsInventory result = inventoryService.adjustToTotal(dim(), 100,
                WmsInventoryBizTypeEnum.STOCKTAKING_ADJUST, "ST002", "无差异");

        assertEquals(100, result.getTotalCount());
        verify(inventoryMapper, never()).updateCounts(anyLong(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(inventoryLogMapper, never()).insert(ArgumentMatchers.<WmsInventoryLog>any());
    }

    @Test
    void createShouldInsertZeroRowWhenDimensionMissing() {
        when(inventoryMapper.selectByDimensionForUpdate(any()))
                .thenReturn(null)
                .thenReturn(row(0, 0, 0, 0));
        when(inventoryMapper.updateCounts(anyLong(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(1);

        WmsInventory result = inventoryService.applyChange(dim(), 10, 0, 0,
                WmsInventoryBizTypeEnum.INBOUND_PUTAWAY, "IN002", "首次入库");

        assertEquals(10, result.getTotalCount());
        verify(inventoryMapper).insert(ArgumentMatchers.<WmsInventory>any());
    }

    @Test
    void shouldRejectIncompleteDimension() {
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> inventoryService.applyChange(new WmsInventoryDimension().setWarehouseId(1),
                        1, 0, 0, WmsInventoryBizTypeEnum.INBOUND_PUTAWAY, "IN003", "缺维度"));
        assertTrue(ex.getMessage().contains("库存维度不完整"));
    }
}
