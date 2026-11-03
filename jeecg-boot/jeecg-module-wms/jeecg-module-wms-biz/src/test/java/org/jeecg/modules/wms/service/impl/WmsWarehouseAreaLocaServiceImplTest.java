package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 库位主数据单测：
 * 验证存储区/拣货区强制禁止混SKU/混批次，退货不良区允许混放，库位编号校验与容积自动推导。
 */
@ExtendWith(MockitoExtension.class)
public class WmsWarehouseAreaLocaServiceImplTest {

    @Mock
    private WmsWarehouseAreaLocaMapper locaMapper;

    @Mock
    private WmsWarehouseAreaMapper areaMapper;

    @InjectMocks
    private WmsWarehouseAreaLocaServiceImpl locaService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(locaService, "baseMapper", locaMapper);
        ReflectionTestUtils.setField(locaService, "warehouseAreaMapper", areaMapper);
    }

    @Test
    void storageAreaLocaShouldForceNoMixSkuAndBatch() {
        WmsWarehouseArea storageArea = new WmsWarehouseArea()
                .setId(10)
                .setWarehouseId(1)
                .setCategory("STORAGE")
                .setStorageCondition("NORMAL");
        when(areaMapper.selectById(10)).thenReturn(storageArea);
        when(locaMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(locaMapper.insert(any(WmsWarehouseAreaLoca.class))).thenReturn(1);

        // 即使前端或外部传入 allowMixSku = 1, allowMixBatch = 1
        WmsWarehouseAreaLoca loca = new WmsWarehouseAreaLoca()
                .setWarehouseAreaId(10)
                .setLocaCode("01-A-01-01")
                .setAllowMixSku(1)
                .setAllowMixBatch(1);

        boolean result = locaService.save(loca);

        assertTrue(result);
        assertEquals(0, loca.getAllowMixSku(), "存储区库位必须强制禁止混SKU");
        assertEquals(0, loca.getAllowMixBatch(), "存储区库位必须强制禁止混批次");
        assertEquals(1, loca.getWarehouseId(), "自动同步库区所属仓库ID");
        assertEquals("NORMAL", loca.getStorageCondition(), "自动继承库区默认存储条件");
    }

    @Test
    void returnAreaLocaShouldAllowMixSkuAndBatch() {
        WmsWarehouseArea returnArea = new WmsWarehouseArea()
                .setId(20)
                .setWarehouseId(1)
                .setCategory("RETURN");
        when(areaMapper.selectById(20)).thenReturn(returnArea);
        when(locaMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(locaMapper.insert(any(WmsWarehouseAreaLoca.class))).thenReturn(1);

        WmsWarehouseAreaLoca loca = new WmsWarehouseAreaLoca()
                .setWarehouseAreaId(20)
                .setLocaCode("RET-01-01");

        boolean result = locaService.save(loca);

        assertTrue(result);
        assertEquals(1, loca.getAllowMixSku(), "退货不良区库位允许混SKU");
        assertEquals(1, loca.getAllowMixBatch(), "退货不良区库位允许混批次");
    }

    @Test
    void shouldAutoCalculateVolumeFromDimensions() {
        WmsWarehouseArea storageArea = new WmsWarehouseArea()
                .setId(10)
                .setWarehouseId(1)
                .setCategory("STORAGE");
        when(areaMapper.selectById(10)).thenReturn(storageArea);
        when(locaMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(locaMapper.insert(any(WmsWarehouseAreaLoca.class))).thenReturn(1);

        // 1000mm * 1000mm * 1000mm = 1,000,000,000 mm³ = 1,000,000 cm³
        WmsWarehouseAreaLoca loca = new WmsWarehouseAreaLoca()
                .setWarehouseAreaId(10)
                .setLocaCode("01-A-01-02")
                .setLocaLen(1000)
                .setLocaWidth(1000)
                .setLocaHeight(1000);

        locaService.save(loca);

        assertEquals(1000000, loca.getLocalVolume(), "应自动计算体积为 1,000,000 cm³");
    }

    @Test
    void shouldRejectDuplicateLocaCodeInSameWarehouse() {
        WmsWarehouseArea storageArea = new WmsWarehouseArea()
                .setId(10)
                .setWarehouseId(1)
                .setCategory("STORAGE");
        when(areaMapper.selectById(10)).thenReturn(storageArea);
        when(locaMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        WmsWarehouseAreaLoca loca = new WmsWarehouseAreaLoca()
                .setWarehouseAreaId(10)
                .setLocaCode("01-A-01-01");

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> locaService.save(loca));
        assertTrue(ex.getMessage().contains("已存在相同编号的库位"));
    }
}
