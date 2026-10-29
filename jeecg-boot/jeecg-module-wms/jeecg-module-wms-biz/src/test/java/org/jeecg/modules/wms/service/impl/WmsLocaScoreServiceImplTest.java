package org.jeecg.modules.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.wms.entity.WmsWarehouseArea;
import org.jeecg.modules.wms.entity.WmsWarehouseAreaLoca;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaLocaMapper;
import org.jeecg.modules.wms.mapper.WmsWarehouseAreaMapper;
import org.jeecg.modules.wms.service.IWmsStrategyConfigService;
import org.jeecg.modules.wms.service.IWmsWarehouseAreaLocaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WmsLocaScoreServiceImplTest {

    @Mock
    private WmsWarehouseAreaMapper warehouseAreaMapper;

    @Mock
    private WmsWarehouseAreaLocaMapper warehouseAreaLocaMapper;

    @Mock
    private IWmsWarehouseAreaLocaService warehouseAreaLocaService;

    @Mock
    private IWmsStrategyConfigService strategyConfigService;

    @InjectMocks
    private WmsLocaScoreServiceImpl locaScoreService;

    @BeforeEach
    void setUp() {
        when(strategyConfigService.getDoubleConfig(eq(1), eq("loca_weight_distance"), anyDouble())).thenReturn(0.6);
        when(strategyConfigService.getDoubleConfig(eq(1), eq("loca_weight_ergonomic"), anyDouble())).thenReturn(0.4);
        when(strategyConfigService.getDoubleConfig(eq(1), eq("loca_a_ratio"), anyDouble())).thenReturn(0.20);
        when(strategyConfigService.getDoubleConfig(eq(1), eq("loca_b_ratio"), anyDouble())).thenReturn(0.30);
    }

    @Test
    void testRecalculateWarehouseLocaScore() {
        // 1. Mock eligible areas: STORAGE (id=10) and PICKING (id=20)
        WmsWarehouseArea storageArea = new WmsWarehouseArea().setId(10).setWarehouseId(1).setCategory("STORAGE");
        WmsWarehouseArea pickingArea = new WmsWarehouseArea().setId(20).setWarehouseId(1).setCategory("PICKING");
        when(warehouseAreaMapper.selectList(any())).thenReturn(Arrays.asList(storageArea, pickingArea));

        // 2. Mock 10 locations with varying pick_distance and ergonomic levels
        // d_max should be 100
        List<WmsWarehouseAreaLoca> locas = new ArrayList<>();
        // Loca 1: dist=10, ergo=1 (golden) -> score = 0.6*(1 - 10/100) + 0.4*1.0 = 0.6*0.9 + 0.4 = 0.54 + 0.40 = 0.94
        locas.add(new WmsWarehouseAreaLoca().setId(1).setWarehouseId(1).setWarehouseAreaId(10).setLocaCode("L-01").setPickDistance(10).setErgonomicLevel(1));
        // Loca 2: dist=20, ergo=1 (golden) -> score = 0.6*0.8 + 0.4*1.0 = 0.48 + 0.40 = 0.88
        locas.add(new WmsWarehouseAreaLoca().setId(2).setWarehouseId(1).setWarehouseAreaId(10).setLocaCode("L-02").setPickDistance(20).setErgonomicLevel(1));
        // Loca 3: dist=20, ergo=2 (suboptimal) -> score = 0.6*0.8 + 0.4*0.6 = 0.48 + 0.24 = 0.72
        locas.add(new WmsWarehouseAreaLoca().setId(3).setWarehouseId(1).setWarehouseAreaId(20).setLocaCode("L-03").setPickDistance(20).setErgonomicLevel(2));
        // Loca 4: dist=40, ergo=2 (suboptimal) -> score = 0.6*0.6 + 0.4*0.6 = 0.36 + 0.24 = 0.60
        locas.add(new WmsWarehouseAreaLoca().setId(4).setWarehouseId(1).setWarehouseAreaId(20).setLocaCode("L-04").setPickDistance(40).setErgonomicLevel(2));
        // Loca 5: dist=50, ergo=2 (suboptimal) -> score = 0.6*0.5 + 0.4*0.6 = 0.30 + 0.24 = 0.54
        locas.add(new WmsWarehouseAreaLoca().setId(5).setWarehouseId(1).setWarehouseAreaId(10).setLocaCode("L-05").setPickDistance(50).setErgonomicLevel(2));
        // Loca 6: dist=60, ergo=2 (suboptimal) -> score = 0.6*0.4 + 0.4*0.6 = 0.24 + 0.24 = 0.48
        locas.add(new WmsWarehouseAreaLoca().setId(6).setWarehouseId(1).setWarehouseAreaId(10).setLocaCode("L-06").setPickDistance(60).setErgonomicLevel(2));
        // Loca 7: dist=70, ergo=3 (inconvenient) -> score = 0.6*0.3 + 0.4*0.2 = 0.18 + 0.08 = 0.26
        locas.add(new WmsWarehouseAreaLoca().setId(7).setWarehouseId(1).setWarehouseAreaId(20).setLocaCode("L-07").setPickDistance(70).setErgonomicLevel(3));
        // Loca 8: dist=80, ergo=3 (inconvenient) -> score = 0.6*0.2 + 0.4*0.2 = 0.12 + 0.08 = 0.20
        locas.add(new WmsWarehouseAreaLoca().setId(8).setWarehouseId(1).setWarehouseAreaId(20).setLocaCode("L-08").setPickDistance(80).setErgonomicLevel(3));
        // Loca 9: dist=90, ergo=3 (inconvenient) -> score = 0.6*0.1 + 0.4*0.2 = 0.06 + 0.08 = 0.14
        locas.add(new WmsWarehouseAreaLoca().setId(9).setWarehouseId(1).setWarehouseAreaId(10).setLocaCode("L-09").setPickDistance(90).setErgonomicLevel(3));
        // Loca 10: dist=100, ergo=3 (inconvenient) -> score = 0.6*0.0 + 0.4*0.2 = 0.08
        locas.add(new WmsWarehouseAreaLoca().setId(10).setWarehouseId(1).setWarehouseAreaId(10).setLocaCode("L-10").setPickDistance(100).setErgonomicLevel(3));

        when(warehouseAreaLocaMapper.selectList(any())).thenReturn(locas);

        // 3. Execute recalculation
        Map<String, Object> summary = locaScoreService.recalculateWarehouseLocaScore(1);

        assertNotNull(summary);
        assertEquals(10, summary.get("totalCandidateLocations"));
        assertEquals(2, summary.get("countA")); // 10 * 0.20 = 2
        assertEquals(3, summary.get("countB")); // 10 * 0.30 = 3
        assertEquals(5, summary.get("countC")); // remainder 5
        assertEquals(100, summary.get("maxDistance"));

        // 4. Verify batch updates captured
        ArgumentCaptor<List<WmsWarehouseAreaLoca>> captor = ArgumentCaptor.forClass(List.class);
        verify(warehouseAreaLocaService).updateBatchById(captor.capture());

        List<WmsWarehouseAreaLoca> updatedList = captor.getValue();
        assertEquals(10, updatedList.size());

        // Top 2 should be A: id=1 and id=2
        assertEquals(1, updatedList.get(0).getId());
        assertEquals("A", updatedList.get(0).getAbcClass());
        assertEquals(new BigDecimal("0.9400"), updatedList.get(0).getLocaScore());

        assertEquals(2, updatedList.get(1).getId());
        assertEquals("A", updatedList.get(1).getAbcClass());
        assertEquals(new BigDecimal("0.8800"), updatedList.get(1).getLocaScore());

        // Next 3 should be B: id=3, id=4, id=5
        assertEquals(3, updatedList.get(2).getId());
        assertEquals("B", updatedList.get(2).getAbcClass());
        assertEquals(new BigDecimal("0.7200"), updatedList.get(2).getLocaScore());

        assertEquals(4, updatedList.get(3).getId());
        assertEquals("B", updatedList.get(3).getAbcClass());
        assertEquals(new BigDecimal("0.6000"), updatedList.get(3).getLocaScore());

        assertEquals(5, updatedList.get(4).getId());
        assertEquals("B", updatedList.get(4).getAbcClass());
        assertEquals(new BigDecimal("0.5400"), updatedList.get(4).getLocaScore());

        // Last 5 should be C
        for (int i = 5; i < 10; i++) {
            assertEquals("C", updatedList.get(i).getAbcClass());
        }
    }
}
