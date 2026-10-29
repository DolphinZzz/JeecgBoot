package org.jeecg.modules.wms.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.entity.WmsWarehouse;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 仓库 / 库区删除的层级保护校验：避免删除后留下孤儿库区与库位
 */
@ExtendWith(MockitoExtension.class)
public class WmsWarehouseCascadeGuardTest {

    @Mock
    private WmsWarehouseMapper warehouseMapper;

    @Mock
    private WmsWarehouseAreaMapper warehouseAreaMapper;

    @Mock
    private WmsWarehouseAreaLocaMapper warehouseAreaLocaMapper;

    @InjectMocks
    private WmsWarehouseServiceImpl warehouseService;

    @InjectMocks
    private WmsWarehouseAreaServiceImpl warehouseAreaService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(warehouseService, "baseMapper", warehouseMapper);
        ReflectionTestUtils.setField(warehouseService, "warehouseAreaMapper", warehouseAreaMapper);
        ReflectionTestUtils.setField(warehouseAreaService, "baseMapper", warehouseAreaMapper);
        ReflectionTestUtils.setField(warehouseAreaService, "warehouseAreaLocaMapper", warehouseAreaLocaMapper);
    }

    @Test
    void removeWarehouseShouldFailWhenAreaExists() {
        when(warehouseAreaMapper.selectCount(any())).thenReturn(2L);

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> warehouseService.removeById(1));
        assertTrue(ex.getMessage().contains("请先删除库区"));
        verify(warehouseMapper, never()).deleteById(anyInt());
    }

    @Test
    void removeWarehouseShouldSucceedWhenNoArea() {
        when(warehouseAreaMapper.selectCount(any())).thenReturn(0L);
        when(warehouseMapper.deleteById(anyInt())).thenReturn(1);

        assertTrue(warehouseService.removeById(1));
        verify(warehouseMapper).deleteById(1);
    }

    @Test
    void removeWarehouseBatchShouldFailWhenAnyAreaExists() {
        when(warehouseAreaMapper.selectCount(any())).thenReturn(1L);

        assertThrows(JeecgBootException.class, () -> warehouseService.removeByIds(Arrays.asList(1, 2)));
        verify(warehouseMapper, never()).deleteBatchIds(any());
    }

    @Test
    void removeAreaShouldFailWhenLocaExists() {
        when(warehouseAreaLocaMapper.selectCount(any())).thenReturn(5L);

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> warehouseAreaService.removeById(10));
        assertTrue(ex.getMessage().contains("请先删除库位"));
        verify(warehouseAreaMapper, never()).deleteById(anyInt());
    }

    @Test
    void removeAreaShouldSucceedWhenNoLoca() {
        when(warehouseAreaLocaMapper.selectCount(any())).thenReturn(0L);
        when(warehouseAreaMapper.deleteById(anyInt())).thenReturn(1);

        assertTrue(warehouseAreaService.removeById(10));
        verify(warehouseAreaMapper).deleteById(10);
    }

    @Test
    void moveAreaToAnotherWarehouseShouldSyncLocaWarehouseId() {
        WmsWarehouseArea old = new WmsWarehouseArea().setId(10).setWarehouseId(1).setCategory("STORAGE").setZoneType("GENERAL").setName("库区A");
        when(warehouseAreaMapper.selectById(10)).thenReturn(old);
        when(warehouseAreaMapper.updateById(any(WmsWarehouseArea.class))).thenReturn(1);

        WmsWarehouseArea update = new WmsWarehouseArea().setId(10).setWarehouseId(2).setName("库区A").setCategory("STORAGE").setZoneType("GENERAL");
        assertTrue(warehouseAreaService.updateById(update));

        verify(warehouseAreaLocaMapper).update(isNull(), any());
    }

    @Test
    void updateWarehouseShouldRejectBlankName() {
        WmsWarehouse entity = new WmsWarehouse().setId(1).setName("   ");
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> warehouseService.updateById(entity));
        assertEquals("请输入仓库名称", ex.getMessage());
    }

    @Test
    void updateAreaShouldRejectMissingWarehouse() {
        WmsWarehouseArea entity = new WmsWarehouseArea().setId(10).setName("库区A");
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> warehouseAreaService.updateById(entity));
        assertEquals("请选择所属仓库", ex.getMessage());
    }

    @Test
    void saveAreaShouldApplyDefaults() {
        when(warehouseAreaMapper.insert(any(WmsWarehouseArea.class))).thenReturn(1);

        WmsWarehouseArea entity = new WmsWarehouseArea().setName("  新库区  ").setWarehouseId(1);
        warehouseAreaService.save(entity);

        assertEquals("新库区", entity.getName());
        assertEquals("STORAGE", entity.getCategory());
        assertEquals("GENERAL", entity.getZoneType());
        assertNull(entity.getId());
        assertEquals(1, entity.getIsDel());
    }

    @Test
    void saveWarehouseShouldDefaultTypeAndStatus() {
        when(warehouseMapper.insert(any(WmsWarehouse.class))).thenReturn(1);

        WmsWarehouse entity = new WmsWarehouse().setName("  一号仓  ");
        warehouseService.save(entity);

        assertEquals("一号仓", entity.getName());
        assertEquals("NORMAL", entity.getWarehouseType());
        assertEquals(1, entity.getSort());
        assertEquals(1, entity.getEnableStatus());
    }

    @Test
    void saveWarehouseShouldRejectUnknownType() {
        WmsWarehouse entity = new WmsWarehouse().setName("一号仓").setWarehouseType("UNKNOWN_TYPE");
        assertThrows(JeecgBootException.class, () -> warehouseService.save(entity));
        verify(warehouseMapper, never()).insert(any(WmsWarehouse.class));
    }

    @Test
    void removeByIdsWithEmptyListShouldNotQuery() {
        List<Object> empty = List.of();

        warehouseService.removeByIds(empty);

        verify(warehouseAreaMapper, never()).selectCount(any());
    }
}
